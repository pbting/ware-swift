package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.charset.Charset;

/**
 *
 */
public class RemotingReceiveLeaderElectionEventListener
        extends AbstractRemotingPipelineEventListener {

    private final static InternalLogger logger = InternalLoggerFactory
            .getInstance(RemotingLeaderElectionVoteEventListener.class);

    public RemotingReceiveLeaderElectionEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive wareSwiftInteractive = event.getValue();
        InteractivePayload interactivePayload = wareSwiftInteractive.getInteractivePayload();

        byte[] result = interactivePayload.getPayload().toByteArray();
        try {
            NodeInformation leader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(result, NodeInformation.class);
            logger.debug("receive leader election result " + leader.toString());
            WareSwiftConfig wareSwiftConfig = RemotingManager.getRemotingManager().getWareSwiftConfig();

            if (!leader.identify()
                    .equalsIgnoreCase(wareSwiftConfig.getNodeInformation().identify())) {
                // 更新当前节点的角色
                wareSwiftConfig.setLeader(leader);
                if (leader.getTerm() > 1) {
                    AbstractRemotingChannel leaderRemotingChannel = RemotingManager
                            .getRemotingManager()
                            .getRemotingChannel(interactivePayload.getSource());
                    if (leaderRemotingChannel == null) {
                        logger.warn(wareSwiftConfig.getNodeInformation().identify() + "; does not connect to the leader " + leader.identify());
                        AbstractRemotingChannel abstractRemotingChannel = RemotingManager
                                .getRemotingManager().getRemotingChannelFactory()
                                .newRemotingChannel(leader.getAddressPort(), leader.getClusterName(), AbstractRemotingChannel.JoinType.NODE_MEET);
                        if (!RemotingManager
                                .getRemotingManager().addRemotingChannel(abstractRemotingChannel)) {
                            abstractRemotingChannel.close();
                        }
                    }
                }
            } else {
                logger.info("is same to self receive leader election .receive is "
                        + leader.identify() + " and self is "
                        + RemotingManager.getRemotingManager().getWareSwiftConfig().getLeader()
                        .identify());
            }

            // 回复 ACK
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setPayload(
                    ByteString.copyFrom(RemotingInteractiveConstants.RECEIVE_LEADER_ACK
                            .getBytes(Charset.forName("UTF-8"))));
            boolean sendSuccess = wareSwiftInteractive.sendPayload(builder.build());
            logger.debug("receive leader election result " + leader.toString()
                    + " ACK send :" + sendSuccess);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
