package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.remoting.*;
import com.alibaba.aliware.core.swift.remoting.avalispart.IAvailableCapabilityModel;
import com.alibaba.aliware.core.swift.remoting.conspart.IConsistenceCapabilityModel;
import com.alibaba.aliware.swift.proto.InteractivePayload;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public abstract class NodeTimeoutCheckEventListenerSupport<V>
        extends AbstractLocalPipelineEventListener<V> {

    public NodeTimeoutCheckEventListenerSupport(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    /**
     * Follower 节点在接收到 Leader 节点发送过来的心跳后，基于 CP 能力模型的需要检测二阶段需要提交的数据。然后进行业务层代码的回调。
     * <p>
     * 基于 AP 能力模型的，因为收到数据就直接对业务方可见，没有二阶段提交。因此这里无需处理 AP 能力模型下的
     * {@link IAvailableCapabilityModel#onInboundDataSet(RemotingDomain, Map)}
     *
     * @param heartbeatPayload
     */
    public void processHeartbeatPayload(final InteractivePayload heartbeatPayload) {
        ICapabilityModel capabilityModel = RemotingManager.getRemotingManager()
                .getCapabilityModel();
        if (capabilityModel instanceof IAvailableCapabilityModel) {
            return;
        }

        final IConsistenceCapabilityModel consistenceCapabilityModel = (IConsistenceCapabilityModel) capabilityModel;
        // 走到这里已经是最新的 leader 信息了。
        final NodeInformation receiveNode = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                .decodeResult(heartbeatPayload.getPayload().toByteArray(),
                        NodeInformation.class);
        // 包含需要处理 committed 的 remoting domain ids. 异步通知业务
        RemotingManager.getRemotingManager().getEventLoopGroup()
                .getParallelQueueExecutor().executeOneTime(() -> {
            final Collection<RemotingDomain> remotingDomains = RemotingManager
                    .getRemotingManager()
                    .committedSyncingDomains(receiveNode.getTerm());
            consistenceCapabilityModel.onCommitted(remotingDomains);
        });
    }
}
