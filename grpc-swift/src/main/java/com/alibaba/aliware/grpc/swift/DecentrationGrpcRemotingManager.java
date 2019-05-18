package com.alibaba.aliware.grpc.swift;

import com.alibaba.aliware.core.swift.NodeState;
import com.alibaba.aliware.core.swift.remoting.HeartbeatMultiMailbox;
import com.alibaba.aliware.core.swift.remoting.IDecentrationRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IMailbox;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.event.local.DecentrationInitRemotingChannelEventListener;
import com.alibaba.aliware.core.swift.remoting.event.local.DecentrationNodeMeetEventListener;
import com.alibaba.aliware.core.swift.remoting.event.local.InitRemotingChannelEventListener;
import com.alibaba.aliware.swift.proto.InteractivePayload;

import java.util.Hashtable;

/**
 * 提供去中心化能力的 grpc remoting manager
 */
public class DecentrationGrpcRemotingManager extends GrpcRemotingManagerSupport
        implements IDecentrationRemotingManager {

    private IMailbox<InteractivePayload> mailbox = new HeartbeatMultiMailbox(
            this.getClass().getName());

    private Hashtable<String, IMailbox> nodesHeartbeatMailboxRegistry = new Hashtable<>();

    @Override
    public void initEventListener() {
        // 1、
        initStartupEventListener();
        // 2、
        initSayHelloEventListener();
        // 3、
        initSendHeartbeatEventListener();
        // 4、
        initTimeoutCheckEventListener();
        // 5、
        initBroadcastSdownEventListener();
        // 6、
        initNodeMeetEventListener();
        // 7、
        initBroadcastNewNodeEventListener();
        //
        initEventPartitioner();
    }

    @Override
    public void initNodeMeetEventListener() {
        remotingEventLoopGroup.addLast(new DecentrationNodeMeetEventListener(this),
                NODE_MEET_EVENT_TYPE);
    }

    @Override
    public InitRemotingChannelEventListener initRemotingChannelEventListener() {

        return new DecentrationInitRemotingChannelEventListener(this);
    }

    @Override
    public void initSayHelloEventListener() {
        // nothing to do
    }

    @Override
    public <T> void setServer(T server) {
        super.setServer(server);
        getRaftConfig().getNodeInformation().setNodeState(NodeState.PEER_TO_PEER);
    }

    @Override
    public String getChannelIdentify(String key) {

        return RemotingInteractiveConstants.PTP_ROLE_PRIFIX + "@" + key;
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
    public void producer(InteractivePayload value) {

        DecentrationRemotingManager.DEFAULT_IMPL.producer(value, nodesHeartbeatMailboxRegistry);
    }

    @Override
    public void addHeartbeatTimeoutCheckMailbox(IMailbox<InteractivePayload> mailbox) {

        nodesHeartbeatMailboxRegistry.put(mailbox.identify(), mailbox);
    }

    @Override
    public void processClusterNodes(String nodes) {

        DecentrationRemotingManager.DEFAULT_IMPL.processClusterNodes(this, nodes);
    }

}
