package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
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
            logger.info(String.format("receive sdown for %s from %s Follower .",
                    sdownLeader.identify(), payload.getSource()));
            String identify = RemotingManager.getRemotingManager().getWareSwiftConfig()
                    .getLeader().identify();
            event.getValue()
                    .sendPayload(InteractivePayload.newBuilder()
                            .setPayload(
                                    ByteString.copyFrom(RemotingInteractiveConstants.PONG
                                            .getBytes(CharsetUtil.UTF_8)))
                            .build());

            if (sdownLeader.identify().equals(identify)) {
                RemotingManager.getRemotingManager().getWareSwiftConfig().getLeader()
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
