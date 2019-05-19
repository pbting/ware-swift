package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannelModel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;

/**
 * 不管是 去中心化还是 Leader-Follower ，当一个新的节点加入到集群中来的时候，每个节点都有能力感知到新加入的节点。
 * <p>
 * 这个 event listener 触发的场景是，当一个新加入的节点和其他节点建立连接之后：
 * <p>
 * Leader-Follower：在指定的 timeout 之后没有收到 leader 发送过来的心跳，并且确认当前这个节点不在 leader 的 follower 节点注册列表中，
 * <p>
 * 这个时候会和 leader 节点触发一次 meet 的操作，leader 收到 meet后，将新加入的节点广播给集群中其他的节点。
 */
public class BroadcastNewNodeEventListener
        extends AbstractLocalPipelineEventListener<NodeInformation> {

    public BroadcastNewNodeEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<NodeInformation> event, int listenerIndex) {
        AbstractRemotingChannelModel remotingChannel = (AbstractRemotingChannelModel) event
                .getSource();

        NodeInformation nodeInformation = event.getValue();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setEventType(
                RemotingEventDispatcher.REMOTING_BROADCAST_NEWNODE_EVENT_TYPE);
        builder.setSource(remotingChannel.identify());
        builder.setSink(remotingManager.getChannelIdentify(nodeInformation.identify()));
        builder.setPayload(
                ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .encodingResult(nodeInformation).array()));
        try {
            InteractivePayload response = remotingChannel
                    .requestResponse(builder.build());
            System.err.println("response broadcast new node:"
                    + response.getPayload().toStringUtf8());
        } catch (Exception e) {
            if (remotingChannel.isNetUnavailable(e)) {
                // 等待下一秒发送，直到 channel 可用为止。
                System.err.println(remotingChannel.identify()
                        + " net unavailable for ...." + this.getClass().getName());
            }
            return false;
        }
        return true;
    }
}
