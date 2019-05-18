package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public class RemotingReceiveSdownLeaderEventListener
        extends AbstractRemotingPipelineEventListener {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(RemotingReceiveSdownLeaderEventListener.class);

    public RemotingReceiveSdownLeaderEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        InteractivePayload payload = event.getValue().getInteractivePayload();

        try {
            NodeInformation sdownLeader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(payload.getPayload().toByteArray(),
                            NodeInformation.class);
            // logger.debug(String.format("receive sdown for %s from %s Follower .",
            // sdownLeader.identify(), payload.getSource()));
            System.err.println(String.format("receive sdown for %s from %s Follower .",
                    sdownLeader.identify(), payload.getSource()));
            String identify = RemotingManager.getRemotingManager().getRaftConfig()
                    .getLeader().identify();

            event.getValue()
                    .sendPayload(InteractivePayload.newBuilder()
                            .setPayload(
                                    ByteString.copyFrom(RemotingInteractiveConstants.PONG
                                            .getBytes(CharsetUtil.UTF_8)))
                            .build());

            if (sdownLeader.identify().equals(identify)) {
                RemotingManager.getRemotingManager().getRaftConfig().getLeader()
                        .getSdownVoteCount().incrementAndGet();
                RemotingManager.getRemotingManager().reElectionForLeader();
            } else {
                logger.warn(String.format(
                        "receive sdown for %s from %s Follower does not equal with self %s.",
                        sdownLeader.identify(), payload.getSource(), identify));
            }
        } catch (Exception e) {
        }
        return true;
    }
}
