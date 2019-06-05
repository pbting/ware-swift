package com.ware.swift.grpc;

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
 * 提供去中心化能力的 grpc remoting manager
 */
public class DecentrationGrpcRemotingManager extends GrpcRemotingManagerSupport
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

    @Override
    public void initSayHelloEventListener() {
        // nothing to do
    }

    @Override
    public <T> void setServer(T server) {
        super.setServer(server);
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
