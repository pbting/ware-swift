package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.avalispart.IAvailableCapabilityModel;
import com.ware.swift.core.remoting.conspart.IConsistenceCapabilityModel;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public abstract class NodeTimeoutCheckEventListenerSupport<V>
        extends AbstractLocalPipelineEventListener<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NodeTimeoutCheckEventListenerSupport.class);

    public NodeTimeoutCheckEventListenerSupport(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    /**
     * Follower 节点在接收到 Leader 节点发送过来的心跳后，基于 CP 能力模型的需要检测二阶段需要提交的数据。然后进行业务层代码的回调。
     * <p>
     * 基于 AP 能力模型的，因为收到数据就直接对业务方可见，没有二阶段提交。因此这里无需处理 AP 能力模型下的
     * {@link IAvailableCapabilityModel#onInboundDataSet(RemotingDomainSupport, Map)}
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
        long receiveTerm = Long.valueOf(heartbeatPayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        // 包含需要处理 committed 的 remoting domain ids. 异步通知业务
        final Collection<RemotingDomainSupport> remotingDomains = RemotingManager
                .getRemotingManager()
                .getTransactionModel().committedSyncingDomains(receiveTerm);

        if (remotingDomains.size() > 0) {
            logger.info("committed data size=" + remotingDomains.size());
            RemotingManager.getRemotingManager().getEventLoopGroup()
                    .getParallelQueueExecutor().executeOneTime(() -> {
                consistenceCapabilityModel.onCommitted(remotingDomains);
            });
        }
    }
}
