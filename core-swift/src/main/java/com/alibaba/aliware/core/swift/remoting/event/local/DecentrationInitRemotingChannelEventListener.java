package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.HeartbeatSingleMailbox;
import com.alibaba.aliware.core.swift.remoting.IMailbox;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

/**
 * 去中心化架构下初始化完 remoting channel 后的一个操作
 */
public class DecentrationInitRemotingChannelEventListener
        extends InitRemotingChannelEventListener {

    public DecentrationInitRemotingChannelEventListener(
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
                new DecentrationNodeTimeoutCheckEventListener(abstractRemotingChannel,
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
        // 5、开始对该节点的数据进行同步
        ClusterDataSyncManager.startDataSyncing(abstractRemotingChannel);
    }

}
