package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;

import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public class RemotingLeaderElectionVoteEventListener
        extends AbstractRemotingPipelineEventListener {

    private final static InternalLogger logger = InternalLoggerFactory
            .getInstance(RemotingLeaderElectionVoteEventListener.class);

    public RemotingLeaderElectionVoteEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive raftInteractive = event.getValue();
        InteractivePayload raftInteractivePayload = raftInteractive
                .getInteractivePayload();
        String source = raftInteractivePayload.getSource();
        logger.debug(String.format("receive vote from %s,and the sink is %s.", source,
                raftInteractivePayload.getSink()));
        IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();

        String implyVoteFor = RemotingManager.getRemotingManager().getImpliedVoteFor();

        if (!StringUtil.isNullOrEmpty(implyVoteFor)) {
            if (implyVoteFor.equals(source)) {
                builder.putHeaders(RemotingInteractiveConstants.LEADER_ELECTION_VOTE_ONE,
                        "1");
            }
        } else if (remotingManager.getRaftConfig().getNodeInformation()
                .setVotedFor(source)) {
            builder.putHeaders(RemotingInteractiveConstants.LEADER_ELECTION_VOTE_ONE,
                    "1");
        }

        logger.debug(raftInteractivePayload.getSink() + " has voted for "
                + remotingManager.getRaftConfig().getNodeInformation().getVotedFor()
                + " success.");
        builder.setSource(raftInteractivePayload.getSink());
        builder.setSink(raftInteractivePayload.getSource());
        return raftInteractive.sendPayload(builder.build());
    }
}
