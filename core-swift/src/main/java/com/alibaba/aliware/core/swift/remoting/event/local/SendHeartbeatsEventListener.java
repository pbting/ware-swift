package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.*;
import com.alibaba.aliware.core.swift.remoting.avalispart.IAvailableCapabilityModel;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.core.swift.remoting.conspart.IConsistenceCapabilityModel;
import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * 不管是在 CP/AP，都需要有向其他 n-1 个节点发送心跳的能力。来检测是否有已经 down 的节点。
 * <p>
 * 去中心化：不管 salve 是 offline 还是 online，都会一直发送心跳给其他 n-1 个节点发送心跳。
 */
public class SendHeartbeatsEventListener
        extends AbstractLocalPipelineEventListener<AbstractRemotingChannel> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(SendHeartbeatsEventListener.class);

    public SendHeartbeatsEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannel> event,
                           int listenerIndex) {
        // 判断是否需要退出心跳
        if (isHeartbeatDown()) {
            onHeartbeatStepDown();
            return true;
        }

        AbstractRemotingChannel remotingChannel = event.getValue();
        System.err.println("\t\theart L=>"
                + remotingManager.getRaftConfig().getNodeInformation().identify()
                + "; R =>" + remotingChannel.identify());
        try {
            onSendHeartbeatSuccess(remotingChannel, sendHeartbeat(remotingChannel,
                    buildInteractivePayload(remotingChannel.identify())));
        } catch (Exception e) {
            logger.error("request/response send heartbeat cause an exception", e);
            // 确实断开连接
            if (remotingChannel.isNetUnavailable(e)) {
                remotingChannel.openOnlineCheckStatus();
                logger.debug(remotingChannel.getAddressPort() + " open online check...");
            }
        }
        return false;
    }

    /**
     * 去中心化的不需要跳过，一直发送心跳即可
     *
     * @return
     */
    public boolean isHeartbeatDown() {

        return false;
    }

    /**
     * @param remotingChannel
     * @param payload
     */
    public InteractivePayload sendHeartbeat(AbstractRemotingChannel remotingChannel,
                                            InteractivePayload payload) throws Exception {
        InteractivePayload response = remotingChannel.requestResponse(payload);
        return response;
    }

    public void onHeartbeatStepDown() {
        // nothing to do
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
        String responsePayload = response.getPayload().toStringUtf8();
        if (!ClusterDataSyncManager.isOutOfCommittedMaxIdleTime(
                remotingChannel.getLastCloseOnlineCheckTime(), 3)) {
            System.err.println("\t is in syncing water marker phase. ");
            return;
        }
        System.err.println("\t\t is out of syncing water marker ,so start to syncing. ");
        ICapabilityModel capabilityModel = RemotingManager.getRemotingManager()
                .getCapabilityModel();

        if (capabilityModel instanceof IConsistenceCapabilityModel) {
            IConsistenceCapabilityModel consistenceCapabilityModel = (IConsistenceCapabilityModel) capabilityModel;
            // 心跳发送成功，对正在同步中的数据进行 committed + 1 的操作。
            final Collection<RemotingDomain> committedRemotingDomains = RemotingManager
                    .getRemotingManager().getCommittedRemotingDomains(
                            processHeartbeatResponse(remotingChannel, responsePayload),
                            remotingChannel);
            if (committedRemotingDomains.size() > 0) {
                RemotingManager.getRemotingManager().getEventLoopGroup()
                        .getParallelQueueExecutor().executeOneTime(() -> {
                    // 如果实现的是一致性的协议。
                    consistenceCapabilityModel
                            .onCommitted(committedRemotingDomains);
                });
            }
        } else if (capabilityModel instanceof IAvailableCapabilityModel) {
            remotingChannel.clearAndGetAllSyncingRemotingDomains()
                    // 重新发送
                    .forEach(interactivePayload -> remotingChannel
                            .requestResponse(interactivePayload));
        }
    }

    public Collection<String> processHeartbeatResponse(
            AbstractRemotingChannel remotingChannel, String responsePayload) {
        // 去中心化的暂时还需无额外处理
        return Collections.emptyList();
    }

    /**
     * @param sink
     * @return
     */
    public InteractivePayload buildInteractivePayload(String sink) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSource(remotingManager.getChannelIdentify(
                remotingManager.getRaftConfig().getNodeInformation().identify()));
        builder.setSink(sink);
        builder.setEventType(
                RemotingEventDispatcher.REMOTING_RECEIVE_HEART_BEAT_EVENT_TYPE);
        customerBuilder(builder);
        builder.setPayload(
                ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .encodingResult(
                                remotingManager.getRaftConfig().getNodeInformation())
                        .array()));
        return builder.build();
    }

    /**
     * 自定义一些发送心跳时所需要携带的 header 数据
     *
     * @param builder
     */
    public void customerBuilder(InteractivePayload.Builder builder) {
        // nothing to do
    }
}
