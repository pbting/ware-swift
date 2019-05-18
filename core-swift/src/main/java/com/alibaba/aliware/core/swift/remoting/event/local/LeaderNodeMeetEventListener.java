package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;

/**
 * 这个 event listener 发生时的行为时：
 * <p>
 * Leader-Follower: 当新加入的节点在 say hello 之后，会得知其他节点返回当前集群的 leader。因此开始启动心跳超时检测。
 * <p>
 * 如果在指定的时间内没有收到 leader 发送过来的心跳，并且再一次 check 当前这个节点不在 leader 节点的 Follower/follower 注册表中，
 * <p>
 * 此时会和 leader 进行一次 meet 的操作。目的就是告诉 leader 节点，当前我是一个新加入的节点。
 * <p>
 * <p>
 * 去中心化: say hello 之后会获取当前集群所有节点的列表，并开始启动心跳超时检测。在指定的时间内没有收到心跳，会和当前这个节点进行一次 meet 的操作。
 */
public class LeaderNodeMeetEventListener
        extends AbstractLocalPipelineEventListener<AbstractRemotingChannel> {

    public LeaderNodeMeetEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannel> event,
                           int listenerIndex) {
        IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        // 表示当前建立的连接中已经包含了 leader 节点。直接发送 leader meet 消息即可
        AbstractRemotingChannel remotingChannel = event.getValue();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setEventType(RemotingEventDispatcher.REMOTING_NODE_MEET_EVENT_TYPE);
        builder.setSink(remotingChannel.identify());
        builder.setSource(remotingManager.getChannelIdentify(
                remotingManager.getRaftConfig().getNodeInformation().identify()));
        try {
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(
                                    remotingManager.getRaftConfig().getNodeInformation())
                            .array()));
            // 返回的是本集群的一批节点信息。
            InteractivePayload interactivePayload = remotingChannel
                    .requestResponse(builder.build());

            String nodes = interactivePayload.getPayload().toStringUtf8();
            System.err.println("receive follower nodes from leader:" + nodes);
            remotingManager.processClusterNodes(nodes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
