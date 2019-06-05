package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;

/**
 * Follower 节点接收 Leader 广播过来的新的节点。
 */
public class RemotingBroadcastNewNodeEventListener
        extends AbstractRemotingPipelineEventListener {

    public RemotingBroadcastNewNodeEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        try {
            NodeInformation nodeInformation = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(event.getValue().getInteractivePayload().getPayload()
                            .toByteArray(), NodeInformation.class);
            AbstractRemotingChannel.addNewNode(nodeInformation.getAddressPort(),
                    nodeInformation.getClusterName(), AbstractRemotingChannel.JoinType.NODE_MEET);
            event.getValue()
                    .sendPayload(InteractivePayload.newBuilder()
                            .setPayload(
                                    ByteString.copyFrom(RemotingInteractiveConstants.PONG
                                            .getBytes(CharsetUtil.UTF_8)))
                            .build());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
