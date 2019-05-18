package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.WareCoreSwiftConfig;
import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;

/**
 *
 */
public class RemotingReceiveHeartbeatEventListener
        extends AbstractRemotingPipelineEventListener {

    public RemotingReceiveHeartbeatEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        final InteractivePayload heartbeatPayload = event.getValue()
                .getInteractivePayload();
        // 往 mailbox 里生产一个消息
        // 注意：这里需要判断网络分区后又重新收到 Leader/ptp 发送过来的心跳这种情况。
        WareCoreSwiftConfig raftConfig = RemotingManager.getRemotingManager().getRaftConfig();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        long defaultTerm = ClusterDataSyncManager.DEFAULT_SYNCING_TERM;
        if (raftConfig.getLeader() != null) {
            long currentTerm = raftConfig.getLeader().getTerm();
            long receiveTerm = Long.valueOf(heartbeatPayload.getHeadersMap()
                    .get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
            if (currentTerm < receiveTerm) {
                // 网络分区的情况。
                NodeInformation leaderNode = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .decodeResult(heartbeatPayload.getPayload().toByteArray(),
                                NodeInformation.class);
                raftConfig.setLeader(leaderNode);
            }
            defaultTerm = currentTerm;
        }
        String syncingDomainIds = RemotingManager.getRemotingManager()
                .prepareCommittedRemotingDomains(heartbeatPayload.getSource(),
                        defaultTerm);
        builder.setPayload(
                ByteString.copyFrom(syncingDomainIds.getBytes(CharsetUtil.UTF_8)));
        RemotingManager.getRemotingManager().producer(heartbeatPayload);
        // ACK
        event.getValue().sendPayload(builder.build());
        return true;
    }
}
