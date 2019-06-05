package com.ware.swift.grpc;

import com.ware.swift.core.remoting.HeartbeatSingleMailbox;
import com.ware.swift.core.remoting.ILeaderFollowerRemotingManager;
import com.ware.swift.core.remoting.IMailbox;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.event.local.InitRemotingChannelEventListener;
import com.ware.swift.core.remoting.event.local.LeaderFollowerInitRemotingChannelEventListener;
import com.ware.swift.core.remoting.event.local.LeaderHeartbeatTimeoutCheckEventListener;
import com.ware.swift.core.remoting.event.local.LeaderSendHeartbeatEventListener;
import com.ware.swift.proto.InteractivePayload;

/**
 * 需要明确知道以下三件事：
 * <p>
 * 1. wareSwift 节点之间的通信入口是哪一个： GrpcRemotingChannel 实例
 * <p>
 * 2. 服务端有数据 response 的客户端入口在哪里: ServerResponseStreamObserver
 * <p>
 * 3. 服务端接收请求之后的处理入口在哪里: ClientRequestStreamObserver
 */
public class LeaderFollowerGrpcRemotingManager extends GrpcRemotingManagerSupport
        implements ILeaderFollowerRemotingManager {

    private IMailbox<InteractivePayload> singleMailbox = new HeartbeatSingleMailbox(
            this.getClass().getName());

    @Override
    public void initEventListener() {

        LeaderFollowerRemotingManager.DEFAULT_IMPL.initEventListener(this);
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
        remotingEventLoopGroup.addListener(new LeaderSendHeartbeatEventListener(this),
                SEND_HEARTBEATS_EVENT_TYPE);
    }

    @Override
    public String getChannelIdentify(String key) {

        return RemotingInteractiveConstants.RAFT_ROLE_PREFIX + "@" + key;
    }

    @Override
    public IMailbox<InteractivePayload> getMailbox() {
        return this.singleMailbox;
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
