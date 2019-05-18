package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;

/**
 * 去中心化时发生的 node meet 的操作。
 * <p>
 * 发生此操作，一般是在对等的节点中还没有包含当前此节点，大多数情况下是在新加入一个节点时发生的行为。
 */
public class DecentrationNodeMeetEventListener
        extends AbstractLocalPipelineEventListener<AbstractRemotingChannel> {

    public DecentrationNodeMeetEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannel> event,
                           int listenerIndex) {

        AbstractRemotingChannel remotingChannel = event.getValue();

        NodeInformation nodeInformation = remotingManager.getRaftConfig()
                .getNodeInformation();

        InteractivePayload response = remotingChannel
                .requestResponse(InteractivePayload.newBuilder()
                        .setSource(remotingManager
                                .getChannelIdentify(nodeInformation.identify()))
                        .setSink(remotingChannel.identify())
                        .setPayload(ByteString.copyFrom(
                                RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                                        .encodingResult(nodeInformation).array()))
                        .setEventType(
                                RemotingEventDispatcher.REMOTING_DECENTRATION_NODE_MEET_EVENT_TYPE)
                        .build());

        String nodes = response.getPayload().toStringUtf8();
        remotingManager.processClusterNodes(nodes);
        return true;
    }
}
