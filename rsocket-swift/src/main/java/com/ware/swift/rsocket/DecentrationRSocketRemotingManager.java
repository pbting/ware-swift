package com.ware.swift.rsocket;

import com.ware.swift.core.NodeState;
import com.ware.swift.core.remoting.HeartbeatMultiMailbox;
import com.ware.swift.core.remoting.IDecentralizeRemotingManager;
import com.ware.swift.core.remoting.IMailbox;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.event.local.DecentralizeInitRemotingChannelEventListener;
import com.ware.swift.core.remoting.event.local.DecentralizeNodeMeetEventListener;
import com.ware.swift.core.remoting.event.local.DecentralizeSendHeartbeatsEventListener;
import com.ware.swift.core.remoting.event.local.InitRemotingChannelEventListener;
import com.ware.swift.proto.InteractivePayload;

import java.util.Hashtable;

/**
 * 提供去中心化能力的 rsocket remoting manager
 */
public class DecentrationRSocketRemotingManager extends RSocketRemotingManagerSupport
        implements IDecentralizeRemotingManager {

    private IMailbox<InteractivePayload> mailbox = new HeartbeatMultiMailbox(
            this.getClass().getName());

    private Hashtable<String, IMailbox> nodesHeartbeatMailboxRegistry = new Hashtable<>();

    @Override
    public void initEventListener() {

        DecentralizeRemotingManager.DEFAULT_IMPL.initEventListener(this);
    }

    @Override
    public void initNodeMeetEventListener() {
        remotingEventLoopGroup.addLast(new DecentralizeNodeMeetEventListener(this),
                NODE_MEET_EVENT_TYPE);
    }

    @Override
    public InitRemotingChannelEventListener initRemotingChannelEventListener() {

        return new DecentralizeInitRemotingChannelEventListener(this);
    }

    /**
     * 去中心化处理节点是否在线的方式
     *
     * @param source
     */
    @Override
    public void isOnlineWithRemotingChannel(String source) {

        this.sendHeartbeatsIfExist(source);
    }

    @Override
    public void initTimeoutCheckEventListener() {
        // nothing to do
    }

    @Override
    public void initSendHeartbeatEventListener() {
        remotingEventLoopGroup.addListener(new DecentralizeSendHeartbeatsEventListener(this),
                SEND_HEARTBEATS_EVENT_TYPE);

    }

    @Override
    public void initSayHelloEventListener() {
        // nothing to do
    }

    @Override
    public <T> void setServer(T server) {
        super.setServer(server);
        // 去中心化的每个节点状态都是对等的。
        getWareSwiftConfig().getNodeInformation().setNodeState(NodeState.PEER_TO_PEER);
    }

    @Override
    public String getChannelIdentify(String key) {
        return RemotingInteractiveConstants.PTP_ROLE_PREFIX + "@" + key;
    }

    @Override
    public IMailbox<InteractivePayload> getMailbox() {
        return mailbox;
    }

    /**
     * 去中心化的心跳检测，每个连接都要
     *
     * @param value
     */
    @Override
    public void producer(InteractivePayload value) {

        DecentralizeRemotingManager.DEFAULT_IMPL.producer(value, nodesHeartbeatMailboxRegistry);
    }

    @Override
    public void addHeartbeatTimeoutCheckMailbox(IMailbox<InteractivePayload> mailbox) {

        nodesHeartbeatMailboxRegistry.put(mailbox.identify(), mailbox);
    }

    @Override
    public void processClusterNodes(String nodes) {

        DecentralizeRemotingManager.DEFAULT_IMPL.processClusterNodes(this, nodes);
    }

}
