package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannelModel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 只有在 Leader-Follower 架构下才有的行为。
 */
public class BroadcastLeaderEventListener
        extends AbstractLocalPipelineEventListener<AbstractRemotingChannelModel> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(BroadcastLeaderEventListener.class);

    public BroadcastLeaderEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannelModel> event,
                           int listenerIndex) {
        AbstractRemotingChannelModel remotingChannel = event.getValue();
        logger.info("-------------->" + remotingChannel.identify() + " "
                + this.getClass().getName());
        try {

            // 将 Leader 信息发送给 Follower 节点。
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSink(remotingChannel.identify());
            builder.setSource(remotingManager.getChannelIdentify(
                    remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
            builder.setEventType(
                    RemotingEventDispatcher.REMOTING_BROADCAST_LEADER_EVENT_TYPE);
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(
                                    remotingManager.getWareSwiftConfig().getNodeInformation())
                            .array()));
            // 同步通知，确保每个 Follower 节点 都受到
            InteractivePayload response = remotingChannel
                    .requestResponse(builder.build());
            logger.debug("receive send leader information response with "
                    + response.getPayload().toStringUtf8() + " from "
                    + remotingChannel.identify());
        } catch (Exception e) {
            logger.error("broadcast leader cause an exception by "
                    + remotingChannel.identify(), e);
            if (remotingChannel.isNetUnavailable(e)) {
                logger.warn(remotingChannel.identify()
                        + " is offline,wait next event loop to check is online and then send the leader 。");
                remotingChannel.openOnlineCheckStatus();
                // 广播失败，会在心跳那里处理当前集群如果出现两个 Leader 的情况。
                return true;
            }
        }

        remotingChannel.closeOnlineCheckStatus();
        // send heartbeats to follower
        RemotingManager.getRemotingManager().sendHeartbeats(remotingChannel);
        return true;
    }
}
