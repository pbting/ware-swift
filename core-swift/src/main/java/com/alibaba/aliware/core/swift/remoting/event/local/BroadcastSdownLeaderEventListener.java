package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
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
        System.err.println("广播....sdown." + sdownLeader.identify());
        remotingManager.getRemotingChannels().forEach(remotingChannel -> {
            if (remotingManager.getChannelIdentify(sdownLeader.identify())
                    .equalsIgnoreCase(remotingChannel.identify())) {
                return;
            }

            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSource(remotingManager.getChannelIdentify(
                    remotingManager.getRaftConfig().getNodeInformation().identify()));
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
