package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 接收 Leader 发送过来的心跳
 */
public class RemotingReceiveLeaderHeartbeatEventListener
        extends RemotingReceiveHeartbeatEventListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RemotingReceiveLeaderHeartbeatEventListener.class);

    @Override
    public InteractivePayload processHeartbeatPayload(InteractivePayload heartbeatPayload) {
        WareSwiftConfig wareSwiftConfig = RemotingManager.getRemotingManager().getWareSwiftConfig();

        long receiveTerm = Long.valueOf(heartbeatPayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        logger.info("Receive Node Identify=" + wareSwiftConfig.getNodeInformation().toString() + "; Receive term=" + receiveTerm);
        // 1. 注意：这里需要判断网络分区后又重新收到 Leader/ptp 发送过来的心跳这种情况。
        long currentTerm = wareSwiftConfig.getLeader().getTerm();
        if (currentTerm < receiveTerm) {
            NodeInformation leaderNode = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(heartbeatPayload.getPayload().toByteArray(),
                            NodeInformation.class);
            wareSwiftConfig.setLeader(leaderNode);
        }

        InteractivePayload.Builder builder = InteractivePayload.newBuilder();

        // 2. 处理是否有数据 committed(被动接受 committed)，以及是否有必要启动数据复制。
        {
            builder.setPayload(
                    ByteString.copyFrom(onPrepareCommittedRemotingDomains(heartbeatPayload).getBytes(CharsetUtil.UTF_8)));
            startDataReplicationIfNecessary(heartbeatPayload);
        }

        // 3. 往 mailbox 里生产一个消息
        RemotingManager.getRemotingManager().producer(heartbeatPayload);
        return builder.build();
    }
}
