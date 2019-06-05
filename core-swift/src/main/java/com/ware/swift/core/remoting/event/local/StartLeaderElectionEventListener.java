package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.NodeState;
import com.ware.swift.core.WareSwiftExceptionCode;
import com.ware.swift.core.WareSwiftGlobalContext;
import com.ware.swift.core.remoting.AbstractRemotingManager;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public class StartLeaderElectionEventListener
        extends AbstractLocalPipelineEventListener<WareSwiftGlobalContext> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(StartLeaderElectionEventListener.class);

    public StartLeaderElectionEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<WareSwiftGlobalContext> event, int listenerIndex) {
        // 第一次 timeout 时间到，将自己设置为 candidate。
        event.setInterrupt(false);
        boolean isStop = false;
        remotingManager.getWareSwiftConfig().getNodeInformation()
                .setNodeState(NodeState.Candidate);

        if (remotingManager.getWareSwiftConfig().getNodeInformation()
                .setVotedFor(remotingManager.getChannelIdentify(remotingManager
                        .getWareSwiftConfig().getNodeInformation().identify()))) {
            logger.debug(String.format("vote for self[%s] success.",
                    remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
            remotingManager.getWareSwiftConfig().getNodeInformation().increVoteCount();
        }

        // 开始要求其他节点投票。同时其他节点会将自己的投票信息给 response 回来。
        for (AbstractRemotingChannel remotingChannel : remotingManager
                .getRemotingChannels()) {
            // 如果已经接收到选举的结果，则不再向别人要求投票
            if (remotingManager.getWareSwiftConfig().getLeader() != null) {
                logger.debug(
                        "has receive the leader election result,so will skip voted by others.");
                // 不需要再执行后面的 listener 了
                event.setInterrupt(true);
                remotingManager.getEventLoopGroup().removeListener(
                        AbstractRemotingManager.LEADER_ELECTION_EVENT_TYPE);
                isStop = true; // 结束。无需 event loop
                // 注意要开启接收 leader 心跳的超时检测
                RemotingManager.getRemotingManager().startLeaderHeartbeatTimeoutCheck();
                break;
            }

            // prepare the vote payload
            InteractivePayload.Builder votePayloadBuilder = InteractivePayload
                    .newBuilder();
            votePayloadBuilder.setSource(remotingManager.getChannelIdentify(
                    remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
            votePayloadBuilder.setSink(remotingChannel.identify());
            votePayloadBuilder.setEventType(
                    RemotingEventDispatcher.REMOTING_LEADER_ELECTION_VOTE_EVENT_TYPE);
            // send the payload to current node
            try {
                logger.debug(
                        " start to vote request to " + remotingChannel.getAddressPort());
                InteractivePayload interactivePayload = remotingChannel
                        .requestResponse(votePayloadBuilder.build());
                // 发送成功了，说明当前这个 channel is ok,change the state
                remotingChannel.closeOnlineCheckStatus();
                // 从 header 中取出具体的投票信息
                String voteCount = interactivePayload.getHeadersMap()
                        .get(RemotingInteractiveConstants.LEADER_ELECTION_VOTE_ONE);
                if (voteCount != null) {
                    // 说明当前发送选举投票的节点还没有像其他节点投票。给自己增加一票
                    remotingManager.getWareSwiftConfig().getNodeInformation().increVoteCount();
                }

                logger.debug("receive the result by vote from "
                        + interactivePayload.getSource()
                        + "; the current node get vote count is :" + remotingManager
                        .getWareSwiftConfig().getNodeInformation().getVoteCount());
            } catch (Exception e) {
                // 可能会出现网络异常:
                logger.error(WareSwiftExceptionCode.formatExceptionMessage(
                        WareSwiftExceptionCode.LEADER_ELECTION_VOTE_ERROR_CODE,
                        String.format("vote cause an exception with %s",
                                remotingChannel.getAddressPort())),
                        e);
            }
        }

        return isStop;
    }
}
