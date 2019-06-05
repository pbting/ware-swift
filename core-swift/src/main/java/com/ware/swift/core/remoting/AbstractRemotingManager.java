package com.ware.swift.core.remoting;

import com.ware.swift.core.*;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.channel.IRemotingChannelFactory;
import com.ware.swift.core.remoting.event.local.*;
import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.loop.AbstractAsyncEventLoopGroup;
import com.ware.swift.event.loop.DefaultEventLoopGroup;
import com.ware.swift.event.loop.EventLoopConstants;
import com.ware.swift.event.parallel.SuperFastParallelQueueExecutor;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public abstract class AbstractRemotingManager
        implements IRemotingManager, IPluginAble, IMailbox<InteractivePayload> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRemotingManager.class);
    protected static final DefaultEventLoopGroup remotingEventLoopGroup = new DefaultEventLoopGroup(
            new SuperFastParallelQueueExecutor(
                    Runtime.getRuntime().availableProcessors() * 2 + 1, "wareSwift-thread"),
            true);

    protected final AtomicLong SEND_IDENTIFY_COUNT = new AtomicLong();

    protected WareSwiftConfig wareSwiftConfig;

    protected WareSwiftGlobalContext wareSwiftGlobalContext;

    protected ICapabilityModel iCapabilityModel = ICapabilityModel
            .getInstance(this.getClass().getClassLoader());

    protected IClientInteractivePayloadHandler clientInteractivePayloadHandler;

    /**
     * wareSwift 节点之间的通信或者数据同步 使用 Remoting Channel 实例对象来发送数据
     */
    private final ConcurrentHashMap<String, AbstractRemotingChannel> remotingChannelRegistry = new ConcurrentHashMap<>();


    private ITransactionModel iTransactionModel = ITransactionModel.getInstance(this.getClass().getClassLoader());

    /**
     * 集群启动的第一个事件类型。
     */
    public static final int START_UP_EVENT_TYPE = 1;
    /**
     * 集群初始化时，给其他节点先送一次 类似 Say Hello 的信息
     */
    public static final int SEND_SAY_HELLO_EVENT_TYPE = 2;
    /**
     * 开始启动 Leader 选举
     */
    public static final int LEADER_ELECTION_EVENT_TYPE = 3;
    /**
     * 向其他节点发送心跳
     */
    public static final int SEND_HEARTBEATS_EVENT_TYPE = 4;
    /**
     * Follower 节点需要定时的检测是否能够在正常的时间范围内接收到 Leader 的心跳
     */
    public static final int START_LEADER_HEARTBEAT_TIMEOUT_CHECK_EVENT_TYPE = 5;
    /**
     * 如果当前节点是 Follower，要将自己认为 Leader 已经下线的状态通知给其他 Follower 节点。
     */
    public static final int BROADCAST_SDOWN_EVENT_TYPE = 6;
    /**
     * 如果一个节点成功的被选举为 Leader，应该要通知其他 n-1 个节点
     */
    public static final int BROADCAST_LEADER_EVENT_TYPE = 7;

    /**
     * 运行过程中，如果扩容一个节点，需要和 leader 进行一次 meet 的操作。
     * <p>
     * 这次操作主要是：
     * <p>
     * 1、通知 leader 节点此时集群中新增了一个节点。并在内存中做一次记录
     * <p>
     * 2、并且需要广播给其他 follower 节点。 follower
     * 节点也需要感知当前集群中新增了一个节点，然后需要和新的节点进行连接。以便为后面实现去中心化的架构做好铺垫
     * <p>
     * 3、leader 接收 meet
     * 消息后，需要将当前集群中所有的节点信息返回给扩容的节点。扩容的节点需要进行一次对比是否有漏掉连接的节点。如果有漏掉的节点，必须新建立一次连接
     */
    public static final int NODE_MEET_EVENT_TYPE = 8;

    /**
     * 当 leader 节点接收到 Follower 节点的 meet 消息时，需要将新增加的节点广播给其他的 follower 节点。
     */
    public static final int BROADCAST_NEW_NODE_EVENT_TYPE = 9;

    /**
     *
     */
    public static final int SYNC_FAIL_EVENT_TYPE = 10;

    /**
     * 去中心化的节点需要定时的检测是否能够在正常的时间范围内接收到 Leader 的心跳。
     * <p>
     * 因为去中心化每个节点都要开启心跳超时检测，因此需要不同的 event type. 以 1000+ 依次往上叠加
     */
    private static AtomicInteger START_HEARTBEAT_TIMEOUT_CHECK_EVENT_TYPE = new AtomicInteger(
            1000);

    /**
     * 用来解决运行过程中重新选举时可能会选举 committed 数最多的那个节点。
     * <p>
     * 正常情况下，所有的节点 committed 数是相等的。主要是处理：Leader -> committed -> Follower,committed 一半，
     * <p>
     * Leader 已经挂了，这个时候我们要确保数据最新的那个节点将会升级为 Leader
     */
    private String implyVoteFor;

    {
        this.clientInteractivePayloadHandler =
                IClientInteractivePayloadHandler
                        .getInstance(this.getClass().getClassLoader());

        clientInteractivePayloadHandler.onAfterConstructInstance();
    }

    public void initEventPartitioner() {
        //
        final List<Integer> partitionerEventList = new LinkedList<>();
        partitionerEventList.add(SEND_SAY_HELLO_EVENT_TYPE);
        partitionerEventList.add(SEND_HEARTBEATS_EVENT_TYPE);
        partitionerEventList.add(BROADCAST_LEADER_EVENT_TYPE);

        remotingEventLoopGroup.registerEventPartitioner(new IEventPartitioner() {
            @Override
            public String partitioner(ObjectEvent objectEvent) {
                if (partitionerEventList.contains(objectEvent.getEventType())) {
                    AbstractRemotingChannel remotingChannel = (AbstractRemotingChannel) objectEvent
                            .getValue();
                    return remotingChannel.getAddressPort();
                }

                return objectEvent.getEventTopic();
            }
        });
    }

    public void initBroadcastNewNodeEventListener() {
        remotingEventLoopGroup.addLast(new BroadcastNewNodeEventListener(this),
                BROADCAST_NEW_NODE_EVENT_TYPE);
    }

    public void initNodeMeetEventListener() {
        remotingEventLoopGroup.addLast(new LeaderNodeMeetEventListener(this),
                NODE_MEET_EVENT_TYPE);
    }

    public void initBroadcastLeaderEventListener() {
        // 启动 Follower 上线检测
        remotingEventLoopGroup.addLast(new BroadcastLeaderEventListener(this),
                BROADCAST_LEADER_EVENT_TYPE);
    }

    public void initBroadcastSdownEventListener() {
        remotingEventLoopGroup.addListener(new BroadcastSdownLeaderEventListener(this),
                BROADCAST_SDOWN_EVENT_TYPE);
    }

    public void initSyncFailEventListener() {
        this.getEventLoopGroup().addLast(new SyncFailEventListener(this),
                SYNC_FAIL_EVENT_TYPE);
    }

    public abstract void initTimeoutCheckEventListener();

    public abstract void initSendHeartbeatEventListener();

    public void initSayHelloEventListener() {
        remotingEventLoopGroup.addListener(
                new LeaderFollowerSendSayHelloEventListener(this),
                SEND_SAY_HELLO_EVENT_TYPE);
    }

    public void initStartupEventListener() {
        remotingEventLoopGroup.addListener(initStartupServerEventListener(),
                START_UP_EVENT_TYPE);
        remotingEventLoopGroup.addListener(initRemotingChannelEventListener(),
                START_UP_EVENT_TYPE);
    }

    /**
     * @return
     */
    public abstract InitRemotingChannelEventListener initRemotingChannelEventListener();

    /**
     *
     */
    public abstract void initEventListener();

    /**
     * @return
     */
    public abstract StartupServerEventListener initStartupServerEventListener();

    public void initLeaderElectionEventListener(boolean isWaitAllReady) {
        remotingEventLoopGroup.addListener(
                new CheckIsStartupLeaderElectionEventListener(isWaitAllReady, this),
                LEADER_ELECTION_EVENT_TYPE);
        remotingEventLoopGroup.addListener(new StartLeaderElectionEventListener(this),
                LEADER_ELECTION_EVENT_TYPE);
        remotingEventLoopGroup.addListener(new BroadcastVoteCountEventListener(this),
                LEADER_ELECTION_EVENT_TYPE);
    }

    public String getPluginName() {
        return this.getClass().getName();
    }

    public boolean init(WareSwiftConfig wareSwiftConfig) {
        initEventListener();
        remotingEventLoopGroup.publish(wareSwiftConfig, START_UP_EVENT_TYPE);
        return true;
    }

    /**
     * 开始节点选举。此方法被执行将会在以下两种情况下触发：
     * <p>
     * 1、集群初始化的时候，所有节点会参与到 leader election 过程中
     * <p>
     * 2、集群运行过程中，如果发现收到 leader 的心跳超时了，也会触发一次 leader 选择。不过这个时候会判断超过半数以上认为 leader 确实已经
     * offline 了，才会启动一次新的选举。
     */
    public void startLeaderElection(WareSwiftGlobalContext wareSwiftGlobalContext) {
        this.wareSwiftGlobalContext = wareSwiftGlobalContext;
        remotingEventLoopGroup.publish(wareSwiftGlobalContext, LEADER_ELECTION_EVENT_TYPE);
    }

    public WareSwiftConfig getWareSwiftConfig() {

        return wareSwiftConfig;
    }

    @Override
    public void sendHeartbeats(AbstractRemotingChannel remotingChannel) {
        if (remotingChannel.getLastSendHeartbeatTime().get() > 0) {
            return;
        }
        ObjectEvent objectEvent = new ObjectEvent(this, remotingChannel,
                SEND_HEARTBEATS_EVENT_TYPE);

        long sendHeartbeatInterval = RemotingManager.getRemotingManager().getWareSwiftConfig()
                .getNodeInformation().getHeartbeatInterval(1000);
        objectEvent.setParameter(EventLoopConstants.EVENT_LOOP_INTERVAL_PARAM,
                sendHeartbeatInterval);
        remotingEventLoopGroup.notifyListeners(objectEvent);
    }

    /**
     * @param sourceIdentifyChannel
     */
    @Override
    public void sendHeartbeatsIfExist(String sourceIdentifyChannel) {
        AbstractRemotingChannel remotingChannel = getRemotingChannel(
                sourceIdentifyChannel);
        if (remotingChannel != null) {
            remotingChannel.closeOnlineCheckStatus();
            sendHeartbeats(remotingChannel);
        }
    }

    @Override
    public void startLeaderHeartbeatTimeoutCheck() {
        ObjectEvent objectEvent = new ObjectEvent(this, this, START_LEADER_HEARTBEAT_TIMEOUT_CHECK_EVENT_TYPE);
        objectEvent.setParameter(EventLoopConstants.EVENT_LOOP_INTERVAL_PARAM, wareSwiftConfig.getNodeInformation().getHeartbeatInterval(1000));
        remotingEventLoopGroup.notifyListeners(objectEvent);
    }

    @Override
    public void producer(InteractivePayload value) {
        this.getMailbox().producer(value);
    }

    @Override
    public InteractivePayload consumer(int timeout, TimeUnit timeUnit) {

        return this.getMailbox().consumer(timeout, timeUnit);
    }

    @Override
    public void broadcastSdown(NodeInformation sdownNode) {

        remotingEventLoopGroup.publish(sdownNode, BROADCAST_SDOWN_EVENT_TYPE);
    }

    @Override
    public void reElectionForLeader() {
        // 检测是否需要重新选举
        if (wareSwiftConfig.getLeader() != null) {
            NodeInformation odown = wareSwiftConfig.getAndResetLeader();
            if (odown != null) {
                odown.setNodeState(NodeState.ODOWN);
                wareSwiftConfig.addODownNode(odown);
            }
        }

        wareSwiftConfig.getNodeInformation().clearVoteStatus();
        // 检测是否开启重新选举
        remotingEventLoopGroup.removeListener(LEADER_ELECTION_EVENT_TYPE);
        initLeaderElectionEventListener(false);
        logger.info("开始新一轮的 Leader 选举..");
        startLeaderElection(wareSwiftGlobalContext);
    }

    @Override
    public abstract IRemotingChannelFactory getRemotingChannelFactory();

    @Override
    public boolean addRemotingChannel(AbstractRemotingChannel remotingChannel) {

        return remotingChannelRegistry.putIfAbsent(remotingChannel.identify(),
                remotingChannel) == null;
    }

    @Override
    public Collection<AbstractRemotingChannel> getRemotingChannels() {
        List<AbstractRemotingChannel> remotingChannels = new ArrayList<>(
                remotingChannelRegistry.values().size());
        remotingChannelRegistry.values().forEach(remotingChannel -> {
            remotingChannels.add(remotingChannel);
        });

        return remotingChannels;
    }

    @Override
    public void broadcastNewNode(final NodeInformation nodeInformation) {
        remotingChannelRegistry.values().forEach(remotingChannel -> {
            if (remotingChannel.identify().contains(nodeInformation.getAddressPort())) {
                // 跳过已经是新增节点。一般这种情况是当前的 leader 节点。
                return;
            }

            ObjectEvent objectEvent = new ObjectEvent(remotingChannel, nodeInformation,
                    BROADCAST_NEW_NODE_EVENT_TYPE);
            remotingEventLoopGroup.notifyListeners(objectEvent);
        });
    }

    @Override
    public boolean isContainsRemotingChannel(String identify) {

        return remotingChannelRegistry.get(identify) != null;
    }

    @Override
    public ITransactionModel getTransactionModel() {

        return this.iTransactionModel;
    }

    @Override
    public int getActiveChannelCount() {
        final AtomicInteger activeCount = new AtomicInteger();
        List<AbstractRemotingChannel> remotingChannels = new LinkedList<>(
                remotingChannelRegistry.values());
        remotingChannels.forEach(remotingChannel -> {
            if (!remotingChannel.isOpenOnlineCheck()) {
                activeCount.incrementAndGet();
            }
        });
        return activeCount.get();
    }

    /**
     * 只有本节点是 leader 才处理。
     *
     * @param source
     */
    @Override
    public void isOnlineWithRemotingChannel(String source) {
        if (wareSwiftConfig.getLeader() == null) {
            return;
        }

        if (!wareSwiftConfig.getLeader().identify()
                .equals(wareSwiftConfig.getNodeInformation().identify())) {
            return;
        }

        this.sendHeartbeatsIfExist(source);
    }

    @Override
    public void setRaftConfig(WareSwiftConfig wareSwiftConfig) {
        this.wareSwiftConfig = wareSwiftConfig;
    }

    @Override
    public AbstractAsyncEventLoopGroup getEventLoopGroup() {
        return remotingEventLoopGroup;
    }

    @Override
    public int getSendIdentifyCount() {
        return (int) SEND_IDENTIFY_COUNT.get();
    }

    @Override
    public void increSendIdentifyCount() {
        SEND_IDENTIFY_COUNT.incrementAndGet();
    }

    @Override
    public AbstractRemotingChannel getRemotingChannel(String identify) {
        return remotingChannelRegistry.get(identify);
    }

    @Override
    public String getChannelIdentify(String key) {

        return key;
    }

    @Override
    public ICapabilityModel getCapabilityModel() {

        return iCapabilityModel;
    }

    @Override
    public IClientInteractivePayloadHandler getClientInteractivePayloadHandler() {

        return clientInteractivePayloadHandler;
    }

    public static int getNextHeartbeatEventType() {

        return START_HEARTBEAT_TIMEOUT_CHECK_EVENT_TYPE.getAndIncrement();
    }

    @Override
    public void implyVoteFor(String implyVoteFor) {

        this.implyVoteFor = implyVoteFor;
    }

    @Override
    public String getImpliedVoteFor() {
        return implyVoteFor;
    }
}
