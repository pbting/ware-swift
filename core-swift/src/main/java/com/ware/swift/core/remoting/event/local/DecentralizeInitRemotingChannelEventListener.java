package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.ObjectEvent;

import java.util.List;

/**
 * 去中心化架构下初始化完 remoting channel 后的一个操作
 */
public class DecentralizeInitRemotingChannelEventListener
        extends InitRemotingChannelEventListener {

    public DecentralizeInitRemotingChannelEventListener(
            IRemotingManager remotingManager) {
        super(remotingManager);
    }

    /**
     * 初始化 remoting channel 之后：
     * <p>
     * 1、启动对该 remoting channel 的心跳超时检测
     * <p>
     * 2、给这个 remoting channel 发送心跳
     *
     * @param abstractRemotingChannel
     */
    @Override
    public void onAfterNewRemotingChannel(
            AbstractRemotingChannel abstractRemotingChannel) {
        int eventType = AbstractRemotingManager.getNextHeartbeatEventType();
        // 1、初始化好心跳超时检测
        remotingManager.getEventLoopGroup().addLast(
                new DecentralizeNodeTimeoutCheckEventListener(abstractRemotingChannel,
                        remotingManager),
                eventType);
        IMailbox singleMailbox = new HeartbeatSingleMailbox(
                abstractRemotingChannel.identify());
        // 2、准备好收到心跳时，得知是哪个 mailbox 收到的心跳
        remotingManager.addHeartbeatTimeoutCheckMailbox(singleMailbox);
        ObjectEvent objectEvent = new ObjectEvent(this, singleMailbox, eventType);
        objectEvent.setEventTopic(abstractRemotingChannel.identify());
        // 3、启动心跳超时检测的任务
        remotingManager.getEventLoopGroup().dispatcher(objectEvent);
        // 4、开始启动给这个节点发送心跳
        remotingManager.sendHeartbeats(abstractRemotingChannel);
    }

    @Override
    public void onBeforeInitFinishRemotingChannel(List<String> clusterNodes, String clusterName) {
        buildDecentralizeTerm();
    }

    @Override
    public void onAfterInitFinishRemotingChannel(List<String> clusterNodes, String clusterName) {
        // nothing to do
    }

    private void buildDecentralizeTerm() {
        IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        String identify = remotingManager.getChannelIdentify(remotingManager.getWareSwiftConfig().getNodeInformation().identify());
        RemotingManager.getRemotingManager()
                .getWareSwiftConfig().getNodeInformation()
                .setDecentralizeTerm(IDecentralizeRemotingManager
                        .DecentralizeRemotingManager.DEFAULT_IMPL.generatorTerm(identify));
    }
}
