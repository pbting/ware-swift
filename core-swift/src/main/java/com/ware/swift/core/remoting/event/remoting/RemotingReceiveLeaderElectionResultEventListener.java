package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareCoreSwiftConfig;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.charset.Charset;

/**
 *
 */
public class RemotingReceiveLeaderElectionResultEventListener
        extends AbstractRemotingPipelineEventListener {

    private final static InternalLogger logger = InternalLoggerFactory
            .getInstance(RemotingLeaderElectionVoteEventListener.class);

    public RemotingReceiveLeaderElectionResultEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive raftInteractive = event.getValue();
        InteractivePayload interactivePayload = raftInteractive.getInteractivePayload();

        byte[] result = interactivePayload.getPayload().toByteArray();
        try {
            NodeInformation leader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(result, NodeInformation.class);
            logger.debug("receive leader election result " + leader.toString());
            WareCoreSwiftConfig raftConfig = RemotingManager.getRemotingManager().getRaftConfig();

            if (!leader.identify()
                    .equalsIgnoreCase(raftConfig.getNodeInformation().identify())) {
                // 更新当前节点的角色
                raftConfig.setLeader(leader);
                if (leader.getTerm() > 1) {
                    AbstractRemotingChannel leaderRemotingChannel = RemotingManager
                            .getRemotingManager()
                            .getRemotingChannel(interactivePayload.getSource());
                    ClusterDataSyncManager.startDataSyncing(leaderRemotingChannel);
                }
            } else {
                System.err.println("is same to self receive leader election .receive is "
                        + leader.identify() + " and self is "
                        + RemotingManager.getRemotingManager().getRaftConfig().getLeader()
                        .identify());
            }

            // 回复 ACK
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setPayload(
                    ByteString.copyFrom(RemotingInteractiveConstants.RECEIVE_LEADER_ACK
                            .getBytes(Charset.forName("UTF-8"))));
            boolean sendSuccess = raftInteractive.sendPayload(builder.build());
            logger.debug("receive leader election result " + leader.toString()
                    + " ACK send :" + sendSuccess);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
