package com.ware.swift.core.remoting.event.local;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.avalispart.IAvailableCapabilityModel;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.conspart.IConsistenceCapabilityModel;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * 不管是在 CP/AP，都需要有向其他 n-1 个节点发送心跳的能力。来检测是否有已经 down 的节点。
 * <p>
 * 去中心化：不管 salve 是 offline 还是 online，都会一直发送心跳给其他 n-1 个节点发送心跳。
 */
public abstract class SendHeartbeatsEventListener
        extends AbstractLocalPipelineEventListener<AbstractRemotingChannel> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(SendHeartbeatsEventListener.class);

    public SendHeartbeatsEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public abstract boolean onEvent(ObjectEvent<AbstractRemotingChannel> event,
                                    int listenerIndex);

    /**
     * @param event
     */
    protected void eventPerform(ObjectEvent<AbstractRemotingChannel> event) {
        AbstractRemotingChannel remotingChannel = event.getValue();
        logger.info("\t\theart L=>"
                + remotingManager.getWareSwiftConfig().getNodeInformation().identify()
                + "; R =>" + remotingChannel.identify());
        InteractivePayload heartbeatPayload = buildHeartbeatPayload(remotingChannel);
        try {
            InteractivePayload responsePayload = sendHeartbeat(remotingChannel, heartbeatPayload);
            onSendHeartbeatSuccess(remotingChannel, responsePayload);
        } catch (Exception e) {
            processException(e, remotingChannel);
        }
    }

    public abstract void processException(Exception e, AbstractRemotingChannel remotingChannel);

    /**
     * @param remotingChannel
     */
    public InteractivePayload sendHeartbeat(AbstractRemotingChannel remotingChannel, InteractivePayload heartbeatPayload) {

        return remotingChannel.requestResponse(heartbeatPayload);
    }

    /**
     * AP 模型下在发送成功之后进行数据同步
     *
     * @param remotingChannel
     * @param response
     */
    public void onSendHeartbeatSuccess(final AbstractRemotingChannel remotingChannel,
                                       InteractivePayload response) {

        remotingChannel.getLastSendHeartbeatTime().set(System.currentTimeMillis());
        remotingChannel.closeOnlineCheckStatus();
        // 心跳发送成功，说明通知对方进行数据复制的消息已经送达，对方收到后就会启动数据的复制
        if (remotingChannel.isStartDataReplication()) {
            remotingChannel.closeStartDataReplication();
        }
        String responsePayload = response.getPayload().toStringUtf8();
        if (!ClusterDataSyncManager.isOutOfCommittedMaxIdleTime(
                remotingChannel.getLastCloseOnlineCheckTime(), 3)) {
            logger.info("\t is in syncing water marker phase. ");
            return;
        }
        ICapabilityModel capabilityModel = RemotingManager.getRemotingManager()
                .getCapabilityModel();

        if (capabilityModel instanceof IConsistenceCapabilityModel) {
            final ITransactionModel transactionModel = RemotingManager.getRemotingManager().getTransactionModel();
            if (!transactionModel.isInDataStreamReplication()) {
                // 复制接收处理 backlog queue 中的数据
                transactionModel.processReplBacklogSyncingQueue();
            }

            final IConsistenceCapabilityModel consistenceCapabilityModel = (IConsistenceCapabilityModel) capabilityModel;
            // 心跳发送成功，对正在同步中的数据进行 committed + 1 的操作。
            final Collection<RemotingDomainSupport> committedRemotingDomains = RemotingManager
                    .getRemotingManager().getTransactionModel().getCommittedRemotingDomains(
                            processHeartbeatResponse(responsePayload),
                            remotingChannel);
            if (committedRemotingDomains.size() > 0) {
                RemotingManager.getRemotingManager().getEventLoopGroup()
                        .getParallelQueueExecutor().executeOneTime(() ->
                        consistenceCapabilityModel
                                .onCommitted(committedRemotingDomains)
                );
            }
        } else if (capabilityModel instanceof IAvailableCapabilityModel) {
            remotingChannel.clearAndGetAllSyncingRemotingDomains()
                    // 重新发送
                    .forEach(interactivePayload -> remotingChannel
                            .requestResponse(interactivePayload));
        }
    }

    public Collection<String> processHeartbeatResponse(String responsePayload) {

        if (ClusterDataSyncManager.DATA_SYNC_EMPTY_COMMITTED_IDS
                .equals(responsePayload)) {
            // 说明当前 发送心跳的节点 term 落后于本节点。
            return Collections.emptyList();
        }

        return Arrays.asList(
                responsePayload.split(ClusterDataSyncManager.SYNCING_IDS_SEPARATOR));
    }

    /**
     * @param remotingChannel
     * @return
     */
    public InteractivePayload buildHeartbeatPayload(AbstractRemotingChannel remotingChannel) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSource(remotingManager.getChannelIdentify(
                remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
        builder.setSink(remotingChannel.identify());
        customerBuilder(builder, remotingChannel);
        builder.setPayload(
                ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .encodingResult(
                                remotingManager.getWareSwiftConfig().getNodeInformation())
                        .array()));
        return builder.build();
    }

    /**
     * 自定义一些发送心跳时所需要携带的 header 数据
     *
     * @param builder
     */
    public void customerBuilder(InteractivePayload.Builder builder, AbstractRemotingChannel remotingChannel) {
        //1. 检测是否需要通知当前发送心跳的节点进行数据同步。
        ITransactionModel transactionModel = RemotingManager.getRemotingManager().getTransactionModel();
        System.out.println("\t data stream replication count =" + transactionModel.getDataStreamReplicationCount());
        if (remotingChannel.isStartDataReplication()) {
            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_DATA_STREAM_REPLICATION, Boolean.toString(true));
        }

        ICapabilityModel capabilityModel = RemotingManager.getRemotingManager().getCapabilityModel();
        if (!(capabilityModel instanceof IConsistenceCapabilityModel)) {
            // 非 CP 能力模型，直接 skip 。
            return;
        }

        // 2. 以下逻辑只会发生在 CP 能力模型的情况下
        final StringBuilder rollbackRemotingDomains = new StringBuilder();
        Collection<RemotingDomainWrapper> semiSyncingRemotingDomains = transactionModel.getAndClearSemiSyncingRemotingDomainIds();
        /**
         * 检测如果从上一次心跳时间到当前心跳时间间隔产生了半同步状态的数据，说明这一时刻会存在脏数据。
         * 即在同步失败的前某个时间段所有节点都已经同步成功。因此也需要进行回滚的操作。
         */
        if (semiSyncingRemotingDomains.size() > 0) {
            // 当前检测到需要回滚数据
            Collection<RemotingDomainWrapper> remotingDomainWrappers = transactionModel.getAndClearSyncingRemotingDomainIds();
            remotingDomainWrappers.forEach(remotingDomainWrapper -> {
                rollbackRemotingDomains.append(remotingDomainWrapper.getSyncingId());
                rollbackRemotingDomains.append(ClusterDataSyncManager.SYNCING_IDS_SEPARATOR);
            });
        }
        semiSyncingRemotingDomains.forEach(remotingDomainWrapper -> {
            rollbackRemotingDomains.append(remotingDomainWrapper.getSyncingId());
            rollbackRemotingDomains.append(ClusterDataSyncManager.SYNCING_IDS_SEPARATOR);
        });

        if (rollbackRemotingDomains.length() > 0) {

            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_SEMI_SYNCING_REMOTING_DOMAIN_ID, rollbackRemotingDomains.toString());
        }
    }
}
