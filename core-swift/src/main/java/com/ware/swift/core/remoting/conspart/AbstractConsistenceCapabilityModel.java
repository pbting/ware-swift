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
 *
 */
public abstract class AbstractConsistenceCapabilityModel
        implements IConsistenceCapabilityModel {

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(AbstractConsistenceCapabilityModel.class);

    /**
     * 处理集群间的数据同步。
     *
     * @param remotingDomain
     * @param headsMap
     * @throws RemotingInteractiveException
     */
    @Override
    public void onInboundDataSet(final RemotingDomain remotingDomain,
                                 Map<String, String> headsMap) {
        // 添加到正在同步 payload 的消息列表中。
        String remotingDomainId = headsMap
                .get(ClusterDataSyncManager.HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID);
        long term = Long
                .valueOf(headsMap.get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        RemotingDomainWrapper remotingDomainWrapper = new RemotingDomainWrapper(
                remotingDomain,
                ClusterDataSyncManager.buildRequireAcksAndGetActiveChannel()[0],
                remotingDomainId, term);
        int size = RemotingManager.getRemotingManager()
                .addSyncingRemotingDoamin(remotingDomainWrapper);
        System.err.println("\t in syncing data size is=" + size);
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
    public void onOutboundDataSet(final RemotingDomain remotingDomain,
                                  OutboundCallback outboundCallback) {
        RemotingManager remotingManager = (RemotingManager) RemotingManager
                .getRemotingManager();

        if (remotingManager.isLeaderFollowerRemotingManager()) {
            NodeInformation leader = RemotingManager.getRemotingManager().getRaftConfig()
                    .getLeader();
            NodeInformation self = RemotingManager.getRemotingManager().getRaftConfig()
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

        final InteractivePayload interactivePayload = ClusterDataSyncManager
                .newSyncInteractivePayload(remotingDomain);

        final Set<String> syncingSuccessChannels = new TreeSet<>();
        final Set<String> syncingFailedChannels = new TreeSet<>();
        String awareMoved = "";
        for (AbstractRemotingChannel remotingChannel : RemotingManager
                .getRemotingManager().getRemotingChannels()) {

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

        // 先写入本地内存。raft 是写入本地磁盘。个人觉得这里可能根据具体的场景来区别是暂写内存还是磁盘。
        String currentDataSyncingId = interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID);
        long term = Long.valueOf(interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        RemotingDomainWrapper remotingDomainWrapper = new RemotingDomainWrapper(
                remotingDomain, requireAcksAndActiveChannel[0], currentDataSyncingId,
                term);
        if (syncingSuccessChannels.size() >= requireAcksAndActiveChannel[0]) {
            RemotingManager.getRemotingManager()
                    .addSyncingRemotingDoamin(remotingDomainWrapper);
            outboundCallback.onSyncingSuccess(syncingSuccessChannels);
        } else {
            RemotingManager.getRemotingManager()
                    .removeSyncingRemotingDoamin(remotingDomainWrapper);
            // 回调同步有失败的情况
            outboundCallback.onSyncingFailed(syncingSuccessChannels,
                    syncingFailedChannels);
        }
    }

}
