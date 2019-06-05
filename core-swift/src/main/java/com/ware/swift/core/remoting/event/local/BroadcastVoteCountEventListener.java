package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.WareSwiftGlobalContext;
import com.ware.swift.core.remoting.AbstractRemotingManager;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.ObjectEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public class BroadcastVoteCountEventListener
        extends AbstractLocalPipelineEventListener<WareSwiftGlobalContext> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(BroadcastVoteCountEventListener.class);

    public BroadcastVoteCountEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<WareSwiftGlobalContext> event, int listenerIndex) {
        // 选举完之后，根据自己的投票数，是否大多数(过半)。如果过半，就无需将自己的票数情况广播给其他节点，直接通知其他节点自己为 leader
        if (remotingManager.getActiveChannelCount() == 0 || remotingManager
                .getWareSwiftConfig().getNodeInformation()
                .getVoteCount() > (remotingManager.getActiveChannelCount()) / 2 + 1) {
            event.setInterrupt(true);
            WareSwiftConfig wareSwiftConfig = remotingManager.getWareSwiftConfig();
            wareSwiftConfig.getNodeInformation().increTerm();
            wareSwiftConfig.setLeader(remotingManager.getWareSwiftConfig().getNodeInformation());
            logger.debug(remotingManager.getWareSwiftConfig().getNodeInformation().identify()
                    + " has become leader,so will broadcast the leader information to other nodes.");

            for (AbstractRemotingChannel remotingChannel : remotingManager
                    .getRemotingChannels()) {
                remotingManager.getEventLoopGroup().publish(remotingChannel,
                        AbstractRemotingManager.BROADCAST_LEADER_EVENT_TYPE);
            }
            return true;
        }

        // 启动接收 Leader 的心跳超时检测,
        /**
         * 1、理想情况下，一次性 Leader 就已经选出来，在 指定的 timeout 时间内，可以正常接收到 leader 发送过来的心跳
         *
         * 2、坏情况下， 一次还没有选出 Leader，那在指定的 timeout 内没有收到心跳信息。自动会进入下一轮的 leader 选举。
         */
        RemotingManager.getRemotingManager().startLeaderHeartbeatTimeoutCheck();
        return true;
    }
}
