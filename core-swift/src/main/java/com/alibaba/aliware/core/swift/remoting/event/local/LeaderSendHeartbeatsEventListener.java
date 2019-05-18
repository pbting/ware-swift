package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.NodeState;
import com.alibaba.aliware.core.swift.remoting.ICapabilityModel;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.core.swift.remoting.conspart.IConsistenceCapabilityModel;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * 不管是在 CP/AP，都需要有向其他 n-1 个节点发送心跳的能力。来检测是否有已经 down 的节点。
 * <p>
 * 改发送心跳任务只要一启动：
 * <p>
 * Leader-Follower：只要 Leader 一直 online，不管 salve 是 offline 还是 online，都会一直发送心跳
 */
public class LeaderSendHeartbeatsEventListener extends SendHeartbeatsEventListener {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(LeaderSendHeartbeatsEventListener.class);

    public LeaderSendHeartbeatsEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    /**
     * Leader-Follower 架构，发送心跳的一定是 leader 节点。leader 节点 和本节点的信息是一致的。
     * <p>
     * 不一致就直接 skip 掉。
     *
     * @return
     */
    @Override
    public boolean isHeartbeatDown() {
        NodeInformation nodeInformation = remotingManager.getRaftConfig()
                .getNodeInformation();
        if (NodeState.Leader != nodeInformation.getNodeState()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        remotingManager.getRaftConfig().getNodeInformation().toString()
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
    @Override
    public void onHeartbeatStepDown() {
        ICapabilityModel capabilityModel = RemotingManager.getRemotingManager()
                .getCapabilityModel();
        if (capabilityModel instanceof IConsistenceCapabilityModel) {
            // 走的是 CP 模型，则
            RemotingManager.getRemotingManager().clearSyncingRemotingDomainIds();
        }
    }

    @Override
    public Collection<String> processHeartbeatResponse(
            AbstractRemotingChannel remotingChannel, String responsePayload) {

        if (ClusterDataSyncManager.DATA_SYNC_EMPTY_COMMITTED_IDS
                .equals(responsePayload)) {
            // 说明当前 发送心跳的节点 term 落后于本节点。
            return Collections.emptyList();
        }

        // 处理网络分区后的重组数据同步问题。这里是使用全量同步还是增量同步，需要好处理。
        return Arrays.asList(
                responsePayload.split(ClusterDataSyncManager.SYNCING_IDS_SEPARATOR));
    }

    @Override
    public void customerBuilder(InteractivePayload.Builder builder) {
        super.customerBuilder(builder);
        builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE,
                String.valueOf(RemotingManager.getRemotingManager().getRaftConfig()
                        .getLeader().getTerm()));
    }

    @Override
    public InteractivePayload sendHeartbeat(AbstractRemotingChannel remotingChannel,
                                            InteractivePayload payload) throws Exception {
        return super.sendHeartbeat(remotingChannel, payload);
    }

}
