package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.NodeState;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.core.remoting.ICapabilityModel;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.conspart.IConsistenceCapabilityModel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 不管是在 CP/AP，都需要有向其他 n-1 个节点发送心跳的能力。来检测是否有已经 down 的节点。
 * <p>
 * 改发送心跳任务只要一启动：
 * <p>
 * Leader-Follower：只要 Leader 一直 online，不管 salve 是 offline 还是 online，都会一直发送心跳
 */
public class LeaderSendHeartbeatEventListener extends SendHeartbeatsEventListener {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(LeaderSendHeartbeatEventListener.class);

    public LeaderSendHeartbeatEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannel> event, int listenerIndex) {
        // 判断是否需要退出心跳
        if (isHeartbeatDown()) {
            // 可能经过网络分区后收到 term 比自己的大，因此当前 leader 退出给其他节点发送心跳，接收最新 term 值节点的心跳
            onHeartbeatStepDown();
            return true;
        }

        eventPerform(event);
        return false;
    }

    @Override
    public void processException(Exception e, AbstractRemotingChannel remotingChannel) {
        logger.error("request/response send heartbeat cause an exception", e);
        // 确实断开连接
        if (!remotingChannel.isNetUnavailable(e)) {
            return;
        }
        remotingChannel.openOnlineCheckStatus();
        // 网络不可用，下一次重连，并且成功发送心跳后，进行数据的赋值
        remotingChannel.startDataReplication();
        logger.debug(remotingChannel.getAddressPort() + " open online check...");
    }

    /**
     * Leader-Follower 架构，发送心跳的一定是 leader 节点。leader 节点 和本节点的信息是一致的。
     *
     * @return
     */
    public boolean isHeartbeatDown() {
        NodeInformation nodeInformation = remotingManager.getWareSwiftConfig()
                .getNodeInformation();
        if (NodeState.Leader != nodeInformation.getNodeState()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        remotingManager.getWareSwiftConfig().getNodeInformation().toString()
                                + " is skip heartbeat to send.");
            }
            return true;
        }

        return false;
    }

    /**
     * heartbeat skip will do something。
     * <p>
     * 基于 Leader-Follower + CP 模型的架构，可能需要处理网络分区后重组，老的 leader 内存还有在进行同步的数据。
     * <p>
     * 这个时候要进行一次内存可擦除的操作。
     */
    public void onHeartbeatStepDown() {
        ICapabilityModel capabilityModel = RemotingManager.getRemotingManager()
                .getCapabilityModel();
        if (capabilityModel instanceof IConsistenceCapabilityModel) {
            // 走的是 CP 模型，则
            RemotingManager.getRemotingManager().getTransactionModel().clearSyncingRemotingDomainIds();
        }
    }

    @Override
    public void customerBuilder(InteractivePayload.Builder builder, AbstractRemotingChannel remotingChannel) {
        // 1. 处理 term 值
        builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE,
                String.valueOf(RemotingManager.getRemotingManager().getWareSwiftConfig()
                        .getLeader().getTerm()));
        // 2. 设置事件类型
        builder.setEventType(
                RemotingEventDispatcher.REMOTING_RECEIVE_HEART_BEAT_EVENT_TYPE);
        super.customerBuilder(builder, remotingChannel);
    }
}
