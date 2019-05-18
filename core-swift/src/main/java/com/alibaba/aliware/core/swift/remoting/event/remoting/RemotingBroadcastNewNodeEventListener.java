package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
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
                    nodeInformation.getClusterName());
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
