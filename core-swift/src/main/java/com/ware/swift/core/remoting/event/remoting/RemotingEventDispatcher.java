package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.disruptor.DisruptorParallelQueueExecutor;
import com.ware.swift.event.loop.AbstractAsyncEventLoopGroup;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public final class RemotingEventDispatcher
        extends AbstractAsyncEventLoopGroup<IInteractive> {

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(RemotingEventDispatcher.class);

    private static final RemotingEventDispatcher REMOTING_EVENT_DISPATCHER = new RemotingEventDispatcher();

    private RemotingEventDispatcher() {
        super(new DisruptorParallelQueueExecutor(
                Runtime.getRuntime().availableProcessors() * 2 + 1,
                1 << 2048), true);
    }

    public static RemotingEventDispatcher getInstance() {
        return REMOTING_EVENT_DISPATCHER;
    }

    /**
     * 1. 用于集群之间的数据通信，预留的事件类型区间为:[0x01, 0xFF]
     *
     * 2. 用于客户端与集群之间的数据通信，预留的事件类型区间为:[0xFF1,0xFFFF]
     *
     * 此思想来源于当初在游戏公司来区分不同类型的协议包是转发的基本的游戏服务器还是战斗服务器还是聊天服务器。
     *
     * 觉得比较好用，就在这里借鉴了。
     */

    /**
     * 用于 stream 标识的事件类型。每个双向流开始发送的第一个 payload 应该需要给自己的这个 stream 去一个名。
     */
    public static final int REMOTING_SAY_HELLO_EVENT_TYPE = 0x01;

    /**
     * 开始进行投票选取的事件类型
     */
    public static final int REMOTING_LEADER_ELECTION_VOTE_EVENT_TYPE = 0x02;

    /**
     * 每个节点将自己的投票数广播给其他节点。
     */
    public static final int BROADCAST_VOTE_COUNT_EVENT_TYPE = 0x03;

    /**
     * 广播 leader 给其他节点
     */
    public static final int REMOTING_BROADCAST_LEADER_EVENT_TYPE = 0x04;

    /**
     * 处理接收来自 leader 发送的心跳
     */
    public static final int REMOTING_RECEIVE_HEART_BEAT_EVENT_TYPE = 0x05;

    /**
     *
     */
    public static final int REMOTING_RECEIVE_SDOWN_LEADER_EVENT_TYPE = 0x06;

    /**
     * 新增加的 follower 节点，需要和 leader 进行一次 meet。
     */
    public static final int REMOTING_NODE_MEET_EVENT_TYPE = 0x08;

    /**
     * leader 节点往其他 follower 节点广播新增的节点。
     */
    public static final int REMOTING_BROADCAST_NEWNODE_EVENT_TYPE = 0x09;

    /**
     * Follower 节点需要向 Leader 节点确认是否已经在 channel 的注册表中。
     */
    public static final int REMOTING_CHECK_IS_IN_CHANNEL_REGISTRY_EVENT_TYPE = 0xA;

    /**
     * 去中心化时
     */
    public static final int REMOTING_DECENTRATION_SAY_HELLO_EVENT_TYPE = 0x0B;

    /**
     * 去中心化时发送 node meet 的 event type
     */
    public static final int REMOTING_DECENTRATION_NODE_MEET_EVENT_TYPE = 0x0C;

    /**
     * 集群间的数据同步
     */
    public static final int REMOTING_DATA_SYNCING_EVENT_TYPE = 0x0D;

    /**
     * 去中心化架构下的数据同步
     */
    public static final int REMOTING_DECENTRATION_DATA_SYNCING_EVENT_TYPE = 0x0E;

    /**
     *
     */
    public static final int REMOTING_STREAM_DATA_SYNC_EVENT_TYPE = 0x0F;

    /**
     *
     */
    public static final int REMOTING_GET_COMMITTED_EVENT_TYPE = 0x10;

    @Override
    public void attachListener() {
        this.addLast(new RemotingSayHelloEventListener(), REMOTING_SAY_HELLO_EVENT_TYPE);
        this.addLast(new RemotingLeaderElectionVoteEventListener(),
                REMOTING_LEADER_ELECTION_VOTE_EVENT_TYPE);
        this.addLast(new RemotingReceiveLeaderElectionResultEventListener(),
                REMOTING_BROADCAST_LEADER_EVENT_TYPE);
        this.addLast(new RemotingReceiveHeartbeatEventListener(),
                REMOTING_RECEIVE_HEART_BEAT_EVENT_TYPE);
        this.addLast(new RemotingReceiveSdownLeaderEventListener(),
                REMOTING_RECEIVE_SDOWN_LEADER_EVENT_TYPE);
        this.addFirst(new RemotingNodeMeetEventListener(), REMOTING_NODE_MEET_EVENT_TYPE);
        this.addFirst(new RemotingBroadcastNewNodeEventListener(),
                REMOTING_BROADCAST_NEWNODE_EVENT_TYPE);
        this.addFirst(new CheckIsInChannelRegistryEventListener(),
                REMOTING_CHECK_IS_IN_CHANNEL_REGISTRY_EVENT_TYPE);
        this.addLast(new RemotingDecenterationSayHelloEventListener(),
                REMOTING_DECENTRATION_SAY_HELLO_EVENT_TYPE);
        this.addLast(new RemotingDecentrationNodeMeetEventListener(),
                REMOTING_DECENTRATION_NODE_MEET_EVENT_TYPE);
        this.addLast(new RemotingStreamDataSyncEventListener(),
                REMOTING_STREAM_DATA_SYNC_EVENT_TYPE);
        this.addLast(new RuntimeDataSyncEventListener(),
                REMOTING_DATA_SYNCING_EVENT_TYPE);
        this.addLast(new RemotingGetCommittedCountEventListener(),
                REMOTING_GET_COMMITTED_EVENT_TYPE);
        this.setDefaultListener(new RemotingDefaultEventListener());
        logger.info("remoting event listener attach finished.");

        this.registerEventPartitioner(new IEventPartitioner() {
            @Override
            public <V> String partitioner(ObjectEvent<V> objectEvent) {

                IInteractive interactive = (IInteractive) objectEvent.getValue();

                if (REMOTING_RECEIVE_HEART_BEAT_EVENT_TYPE == objectEvent.getEventType()) {

                    return interactive.getInteractivePayload().getSource();
                }

                return objectEvent.getEventTopic();
            }
        });
    }

}
