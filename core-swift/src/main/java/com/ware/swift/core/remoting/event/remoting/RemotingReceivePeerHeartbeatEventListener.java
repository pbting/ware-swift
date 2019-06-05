package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 去中心化架构下接收对等节点发送过来的心跳
 */
public class RemotingReceivePeerHeartbeatEventListener
        extends RemotingReceiveHeartbeatEventListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RemotingReceivePeerHeartbeatEventListener.class);

    @Override
    public InteractivePayload processHeartbeatPayload(InteractivePayload heartbeatPayload) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        String source = heartbeatPayload.getSource();
        if (source.indexOf(RemotingInteractiveConstants.PTP_ROLE_PREFIX) == -1) {
            // 节点不合法
            builder.setPayload(ByteString.copyFrom("Invalid Node Heartbeat.".getBytes(CharsetUtil.UTF_8)));
            return builder.build();
        }

        // 1. 处理是否有数据 committed(被动接受 committed)，以及是否有必要启动数据复制。
        {
            // 去中心化的架构，在接收的心跳中返回当前整个集群的节点信息
            builder.putHeaders(RemotingInteractiveConstants.HEADER_KEY_CLUSTER_NODES, builderRemotingChannels(heartbeatPayload.getSource()).toString());
            builder.setPayload(
                    ByteString.copyFrom(onPrepareCommittedRemotingDomains(heartbeatPayload).getBytes(CharsetUtil.UTF_8)));
            startDataReplicationIfNecessary(heartbeatPayload);
        }

        // 2. 往 mailbox 里生产一个消息
        RemotingManager.getRemotingManager().producer(heartbeatPayload);
        // ACK
        return builder.build();
    }

}
