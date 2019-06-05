package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;

/**
 *
 */
public class BroadcastSdownLeaderEventListener
        extends AbstractLocalPipelineEventListener<NodeInformation> {

    public BroadcastSdownLeaderEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<NodeInformation> event, int listenerIndex) {
        final NodeInformation sdownLeader = event.getValue();
        remotingManager.getRemotingChannels().forEach(remotingChannel -> {
            if (remotingManager.getChannelIdentify(sdownLeader.identify())
                    .equalsIgnoreCase(remotingChannel.identify())) {
                return;
            }

            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSource(remotingManager.getChannelIdentify(
                    remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
            builder.setSink(remotingChannel.identify());
            builder.setEventType(
                    RemotingEventDispatcher.REMOTING_RECEIVE_SDOWN_LEADER_EVENT_TYPE);
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(sdownLeader).array()));
            InteractivePayload sdownResponse;
            try {
                sdownResponse = remotingChannel.requestResponse(builder.build());
                System.out.println(
                        "sdown response :" + sdownResponse.getPayload().toStringUtf8());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
