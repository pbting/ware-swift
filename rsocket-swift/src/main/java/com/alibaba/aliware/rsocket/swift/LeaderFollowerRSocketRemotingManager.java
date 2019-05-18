package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.core.swift.remoting.HeartbeatSingleMailbox;
import com.alibaba.aliware.core.swift.remoting.ILeaderFollowerRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IMailbox;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.event.local.InitRemotingChannelEventListener;
import com.alibaba.aliware.core.swift.remoting.event.local.LeaderFollowerInitRemotingChannelEventListener;
import com.alibaba.aliware.core.swift.remoting.event.local.LeaderHeartbeatTimeoutCheckEventListener;
import com.alibaba.aliware.core.swift.remoting.event.local.LeaderSendHeartbeatsEventListener;
import com.alibaba.aliware.swift.proto.InteractivePayload;

/**
 *
 */
public class LeaderFollowerRSocketRemotingManager extends RSocketRemotingManagerSupport
        implements ILeaderFollowerRemotingManager {

    private IMailbox<InteractivePayload> signelMailbox = new HeartbeatSingleMailbox(
            this.getClass().getName());

    public LeaderFollowerRSocketRemotingManager() {

    }

    @Override
    public void initEventListener() {
        // 1、
        initStartupEventListener();
        // 2、
        initSayHelloEventListener();
        // 3、
        initLeaderElectionEventListener(true);
        // 4、
        initSendHeartbeatEventListener();
        // 5、
        initTimeoutCheckEventListener();
        // 6、
        initBroadcastSdownEventListener();
        // 7、
        initBroadcastLeaderEventListener();
        // 8、
        initNodeMeetEventListener();
        // 9、
        initBroadcastNewNodeEventListener();
        //
        initEventPartitioner();
    }

    @Override
    public InitRemotingChannelEventListener initRemotingChannelEventListener() {

        return new LeaderFollowerInitRemotingChannelEventListener(this);
    }

    @Override
    public void initTimeoutCheckEventListener() {
        remotingEventLoopGroup.addListener(
                new LeaderHeartbeatTimeoutCheckEventListener(this),
                START_LEADER_HEARTBEAT_TIMEOUT_CHECK_EVENT_TYPE);
    }

    @Override
    public void initSendHeartbeatEventListener() {
        remotingEventLoopGroup.addListener(new LeaderSendHeartbeatsEventListener(this),
                SEND_HEARTBEATS_EVENT_TYPE);
    }

    @Override
    public String getChannelIdentify(String key) {
        return RemotingInteractiveConstants.RAFT_ROLE_PRIFIX + "@" + key;
    }

    @Override
    public IMailbox<InteractivePayload> getMailbox() {
        return signelMailbox;
    }

    @Override
    public void addHeartbeatTimeoutCheckMailbox(IMailbox<InteractivePayload> mailbox) {
        throw new UnsupportedOperationException(
                "Leader/Follower does not support add heartbeat timeout check mailbox.");
    }

    @Override
    public void processClusterNodes(String nodes) {
        // 依次处理。没有建立连接的，建立连接。
        LeaderFollowerRemotingManager.DEFAULT_IMPL.processClusterNodes(this, nodes);
    }

}
