package com.ware.swift.core.remoting.conspart;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 提供 CP 能力模型的抽象。如果你的系统需要实现一个 CP 架构的能力，那么实现该基本抽象类是一个不错的选择。
 */
public abstract class AbstractConsistenceCapabilityModel
        implements IConsistenceCapabilityModel {

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(AbstractConsistenceCapabilityModel.class);

    /**
     * 处理集群间的数据同步。
     *
     * @param remotingSource
     * @param remotingDomain
     * @param headsMap
     * @throws RemotingInteractiveException
     */
    @Override
    public void onInboundDataSet(final String remotingSource, final RemotingDomainSupport remotingDomain,
                                 Map<String, String> headsMap) {
        // 添加到正在同步 payload 的消息列表中。
        String remotingDomainId = headsMap
                .get(ClusterDataSyncManager.HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID);
        long term = Long
                .valueOf(headsMap.get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        RemotingDomainWrapper remotingDomainWrapper = new RemotingDomainWrapper(
                remotingDomain,
                ClusterDataSyncManager.buildRequireAcksAndGetActiveChannel()[0],
                remotingDomainId, term, RemotingDomainWrapper.INBOUND_APPLY);
        remotingDomainWrapper.setRemotingSource(remotingSource);
        RemotingManager.getRemotingManager()
                .getTransactionModel().addSyncingRemotingDomain(remotingDomainWrapper);
    }

    /**
     * 数据同步到其他节点。需要考虑以下几个事情:
     * <p>
     * 1.
     *
     * @param remotingDomain
     * @param outboundCallback
     */
    @Override
    public void onOutboundDataSet(final RemotingDomainSupport remotingDomain,
                                  OutboundCallback outboundCallback) {
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        if (RemotingManager.isLeaderFollowerRemotingManager()) {
            NodeInformation leader = remotingManager.getWareSwiftConfig()
                    .getLeader();
            NodeInformation self = remotingManager.getWareSwiftConfig()
                    .getNodeInformation();
            if (!leader.identify().equals(self.identify())) {
                outboundCallback.onMoved(leader.identify());
                return;
            }
        }

        int[] requireAcksAndActiveChannel = ClusterDataSyncManager
                .buildRequireAcksAndGetActiveChannel();

        // 因为是 CP 模型，存活的连接还没有达到 requestRequiredAcks，因此不给于更新。
        if (requireAcksAndActiveChannel[0] > requireAcksAndActiveChannel[1]) {
            outboundCallback.onMinRequestRequiredAcks(requireAcksAndActiveChannel[0],
                    requireAcksAndActiveChannel[1]);
            return;
        }


        ITransactionModel transactionModel = remotingManager.getTransactionModel();
        /**
         * Leader-Follower 架构下，Leader 接收数据的请求，如果发现当前正在数据复制，
         */
        if (transactionModel.isInDataStreamReplication() || transactionModel.getDecentralizeBacklogSwitch()) {
            boolean isAddSuccess = transactionModel.addReplBacklogSyncingQueue(remotingDomain);
            outboundCallback.onReplBacklogSyncingAdd(isAddSuccess, remotingDomain);
            return;
        }

        final InteractivePayload interactivePayload = buildSyncInteractivePayload(remotingDomain);
        final Set<String> syncingSuccessChannels = new TreeSet<>();
        syncingSuccessChannels.add(remotingManager.getChannelIdentify(remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
        final Set<String> syncingFailedChannels = new TreeSet<>();
        String awareMoved = "";
        for (AbstractRemotingChannel remotingChannel : remotingManager.getRemotingChannels()) {

            if (remotingChannel.isOpenOnlineCheck()) {
                continue;
            }

            try {
                InteractivePayload response = remotingChannel
                        .requestResponse(interactivePayload);
                String responsePayload = response.getPayload().toStringUtf8();
                if (responsePayload.indexOf(
                        ClusterDataSyncManager.DATA_SYNC_REASON_PHRASE_MOVED) != -1) {
                    awareMoved = responsePayload.split(" ")[1];
                    break;
                } else {
                    syncingSuccessChannels.add(remotingChannel.identify());
                }
            } catch (Exception e) {
                log.error(remotingChannel.identify() + " syncing data with "
                        + remotingDomain.toString() + " cause an exception.", e);
                syncingFailedChannels.add(remotingChannel.identify());
            }
        }

        if (!StringUtil.isNullOrEmpty(awareMoved)) {
            outboundCallback.onMoved(awareMoved);
            return;
        }

        // 先写入本地内存。wareSwift 是写入本地磁盘。个人觉得这里可能根据具体的场景来区别是暂写内存还是磁盘。
        String currentDataSyncingId = interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID);
        long term = Long.valueOf(interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        RemotingDomainWrapper remotingDomainWrapper = new RemotingDomainWrapper(
                remotingDomain, requireAcksAndActiveChannel[0], currentDataSyncingId,
                term, RemotingDomainWrapper.OUTBOUND_APPLY);
        if (syncingSuccessChannels.size() >= requireAcksAndActiveChannel[0]) {
            remotingDomainWrapper.setSyncingSuccessChannels(syncingSuccessChannels);
            remotingDomainWrapper.setRemotingSource(interactivePayload.getSource());
            remotingManager.getTransactionModel().addSyncingRemotingDomain(remotingDomainWrapper);
            outboundCallback.onSyncingSuccess(syncingSuccessChannels);
        } else {
            transactionModel.addSemiSyncingRemotingDomain(remotingDomainWrapper);
            // 回调同步有失败的情况
            outboundCallback.onSyncingFailed(syncingSuccessChannels,
                    syncingFailedChannels);
        }
    }

}
