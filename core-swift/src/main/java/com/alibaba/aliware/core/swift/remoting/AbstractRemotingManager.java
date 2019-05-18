package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.core.swift.*;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.channel.IRemotingChannelFactory;
import com.alibaba.aliware.core.swift.remoting.event.local.*;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.loop.AbstractAsyncEventLoopGroup;
import com.alibaba.aliware.grpc.swift.event.swift.loop.DefaultEventLoopGroup;
import com.alibaba.aliware.grpc.swift.event.swift.loop.EventLoopConstants;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.SuperFastParallelQueueExecutor;
import com.alibaba.aliware.swift.proto.InteractivePayload;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 *
 */
public abstract class AbstractRemotingManager
        implements IRemotingManager, IPluginable, IMailbox<InteractivePayload> {

    protected static final DefaultEventLoopGroup remotingEventLoopGroup = new DefaultEventLoopGroup(
            new SuperFastParallelQueueExecutor(
                    Runtime.getRuntime().availableProcessors() * 2 + 1, "raft-thread"),
            true);

    protected final AtomicLong SEND_IDENTIFY_COUNT = new AtomicLong();

    protected WareCoreSwiftConfig raftConfig;

    protected WareCoreSwiftGlobalContext raftGlobalContext;

    protected ICapabilityModel iCapabilityModel = ICapabilityModel
            .getInstance(this.getClass().getClassLoader());

    protected IClientInteractivePayloadHandler clientInteractivePayloadHandler = IClientInteractivePayloadHandler
            .getInstance(this.getClass().getClassLoader());

    /**
     * raft 节点之间的通信或者数据同步 使用 Remoting Channel 实例对象来发送数据
     */
    private final ConcurrentHashMap<String, AbstractRemotingChannel> remotingChannelRegistry = new ConcurrentHashMap<>();

    /**
     * 用来记录正在同步中的数据，正在同步中的数据，如果是 CP 模型，此时对所有的客户端是不可见的。
     * <p>
     * 等第二阶段 committed 之后才变为可见。sync-ing -> synced
     * <p>
     * 主要使用来处理 Leader-Follower 架构下的 syncing 状态的数据存储。
     */
    private ConcurrentHashMap<String, RemotingDomainWrapper> syncingRemotingDomainRepository = new ConcurrentHashMap<>();

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
     * Leader 向其他 Follower 节点发送心跳
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

    public abstract void initTimeoutCheckEventListener();

    public void initSendHeartbeatEventListener() {
        remotingEventLoopGroup.addListener(new SendHeartbeatsEventListener(this),
                SEND_HEARTBEATS_EVENT_TYPE);
    }

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

    public boolean init(WareCoreSwiftConfig raftConfig) {
        initEventListener();
        remotingEventLoopGroup.publish(raftConfig, START_UP_EVENT_TYPE);
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
    public void startLeaderElection(WareCoreSwiftGlobalContext raftGlobaleContext) {
        this.raftGlobalContext = raftGlobaleContext;
        remotingEventLoopGroup.publish(raftGlobaleContext, LEADER_ELECTION_EVENT_TYPE);
    }

    public WareCoreSwiftConfig getRaftConfig() {

        return raftConfig;
    }

    @Override
    public void sendHeartbeats(AbstractRemotingChannel remotingChannel) {
        if (remotingChannel.getLastSendHeartbeatTime().get() > 0) {
            return;
        }
        ObjectEvent objectEvent = new ObjectEvent(this, remotingChannel,
                SEND_HEARTBEATS_EVENT_TYPE);

        long sendHeartbeatInterval = RemotingManager.getRemotingManager().getRaftConfig()
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
        System.err.println(" --> sendHeartbeatsIfExist:" + remotingChannel.identify()
                + "; " + remotingChannel.isOpenOnlineCheck());
    }

    @Override
    public void startLeaderHeartbeatTimeoutCheck() {
        remotingEventLoopGroup.publish(this,
                START_LEADER_HEARTBEAT_TIMEOUT_CHECK_EVENT_TYPE);
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
        if (raftConfig.getLeader() != null) {
            NodeInformation odown = raftConfig.getAndResetLeader();
            if (odown != null) {
                odown.setNodeState(NodeState.ODOWN);
                raftConfig.addODownNode(odown);
            }
        }

        raftConfig.getNodeInformation().clearVoteStatus();
        // 检测是否开启重新选举
        remotingEventLoopGroup.removeListener(LEADER_ELECTION_EVENT_TYPE);
        initLeaderElectionEventListener(false);
        System.err.println("开始新一轮的 Leader 选举..");
        startLeaderElection(raftGlobalContext);
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

    /**
     * @param remotingDoaminWrapper
     * @return
     */
    @Override
    public int addSyncingRemotingDoamin(
            final RemotingDomainWrapper remotingDoaminWrapper) {
        syncingRemotingDomainRepository.put(remotingDoaminWrapper.getSyncingId(),
                remotingDoaminWrapper);
        return syncingRemotingDomainRepository.size();
    }

    @Override
    public void visiterSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor) {
        syncingRemotingDomainRepository.values().forEach(visitor);
    }

    @Override
    public void removeSyncingRemotingDoamin(RemotingDomainWrapper remotingDoaminWrapper) {
        syncingRemotingDomainRepository.remove(remotingDoaminWrapper.getSyncingId());
    }

    private Set<String> collectorCommittedIds = new ConcurrentSkipListSet<>();

    /**
     * 对 committed 计数增一，同时收集已经 committed 成功的 remoting domain,和 out of syncing water marker
     *
     * @param remotingChannel
     */
    @Override
    public Collection<RemotingDomain> getCommittedRemotingDomains(
            Collection<String> committedIds, AbstractRemotingChannel remotingChannel) {
        collectorCommittedIds.addAll(committedIds);
        // check
        int size = syncingRemotingDomainRepository.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        Collection<RemotingDomain> committedRemotingDomain = new LinkedList<>();
        final Collection<RemotingDomain> outOfSyncingWaterMarkerRemotingDomain = new LinkedList<>();

        int activeChannel = RemotingManager.getRemotingManager().getActiveChannelCount();
        Set<String> removedIds = new HashSet<>();
        for (RemotingDomainWrapper remotingDomainWrapper : syncingRemotingDomainRepository
                .values()) {

            /**
             * Follower 通过心跳告诉 Leader 当前需要 committed 哪些 domain id. 如果 Leader 发现不在本 syncing
             * 列表中，那说明:
             *
             * 1. 可能 Leader 向 Follower 同步数据的过程中数据丢失了。
             *
             * 2. Follower 还含有之前 Leader 同步过来的数据但是没有收到 之前 Leader 发送过来的 committed 信息 Leader
             * 就已经挂了。
             */
            if (!remotingDomainWrapper.isReSyncCommitted() && !collectorCommittedIds
                    .contains(remotingDomainWrapper.getSyncingId())) {

                if (removeIfOutOfSyncingWaterMarker(remotingDomainWrapper)) {
                    if (remotingDomainWrapper.getSyncingTerm() == RemotingManager
                            .getRemotingManager().getRaftConfig().getLeader().getTerm()) {
                        // re send
                        remotingChannel.fireAndForget(
                                ClusterDataSyncManager.newSyncInteractivePayload(
                                        remotingDomainWrapper.getRemotingDomain()));
                    } else {
                        removedIds.add(remotingDomainWrapper.getSyncingId());
                        outOfSyncingWaterMarkerRemotingDomain
                                .add(remotingDomainWrapper.getRemotingDomain());
                    }
                }

                continue;
            }

            int hasCommittedChannelSize = remotingDomainWrapper
                    .addCommittedRemotingChannel(remotingChannel.identify());

            if (hasCommittedChannelSize >= remotingDomainWrapper
                    .getRequestRequireAcks()) {

                if (remotingDomainWrapper.committed()) {
                    committedRemotingDomain
                            .add(remotingDomainWrapper.getRemotingDomain());
                    RemotingManager.getRemotingManager().getRaftConfig()
                            .getNodeInformation().incrementCommittedCount();
                }

                if (remotingDomainWrapper.getHasCommittedChannelSize() >= activeChannel) {
                    // 说明全部存活的 channel 都已经 committed 成功，
                    removedIds.add(remotingDomainWrapper.getSyncingId());
                }
            }

        }

        if (removedIds.size() > 0) {
            removedIds.forEach(in -> {
                syncingRemotingDomainRepository.remove(in);
                collectorCommittedIds.remove(in);
            });
        }

        if (outOfSyncingWaterMarkerRemotingDomain.size() > 0) {
            getEventLoopGroup().getParallelQueueExecutor()
                    .executeOneTime(() -> getCapabilityModel().onOutOfSyncingWaterMarker(
                            outOfSyncingWaterMarkerRemotingDomain));
        }

        return committedRemotingDomain;
    }

    /**
     * 这个方法通常是 Leader 定时发送心跳时来调用的。Leader 进行数据同步时，需要额外判断 term 的值吗？
     *
     * @return
     */
    @Override
    public String prepareCommittedRemotingDomains(String channelSource, long term) {
        int size = syncingRemotingDomainRepository.size();

        if (size == 0) {
            return ClusterDataSyncManager.DATA_SYNC_EMPTY_COMMITTED_IDS;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (RemotingDomainWrapper remotingDomainWrapper : syncingRemotingDomainRepository
                .values()) {
            if (remotingDomainWrapper.getSyncingTerm() != term) {
                continue;
            }
            remotingDomainWrapper.addCommittedRemotingChannel(channelSource);
            String syncingId = remotingDomainWrapper.getSyncingId();
            stringBuilder
                    .append(syncingId + ClusterDataSyncManager.SYNCING_IDS_SEPARATOR);
        }
        stringBuilder.substring(0, stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    @Override
    public int getSyncingRemotingDomainSize() {
        return syncingRemotingDomainRepository.size();
    }

    @Override
    public void clearSyncingRemotingDomainIds() {
        this.syncingRemotingDomainRepository.clear();
    }

    /**
     * @param term 当前 leader 最新的 term 值大小
     * @return
     */
    @Override
    public Collection<RemotingDomain> committedSyncingDomains(long term) {

        List<RemotingDomain> committedRemotingDomains = new LinkedList<>();
        final List<RemotingDomain> outOfSyncingWaterMarkerRemotingDomains = new LinkedList<>();
        List<String> removeIds = new LinkedList<>();
        for (RemotingDomainWrapper remotingDomainWrapper : syncingRemotingDomainRepository
                .values()) {
            if (remotingDomainWrapper.getSyncingTerm() == term) {
                if (remotingDomainWrapper.getHasCommittedChannelSize() > 0) {
                    committedRemotingDomains
                            .add(remotingDomainWrapper.getRemotingDomain());
                    RemotingManager.getRemotingManager().getRaftConfig()
                            .getNodeInformation().incrementCommittedCount();
                    removeIds.add(remotingDomainWrapper.getSyncingId());
                }
            } else if (removeIfOutOfSyncingWaterMarker(remotingDomainWrapper)) {
                removeIds.add(remotingDomainWrapper.getSyncingId());
                outOfSyncingWaterMarkerRemotingDomains
                        .add(remotingDomainWrapper.getRemotingDomain());
            }
        }

        if (outOfSyncingWaterMarkerRemotingDomains.size() > 0) {
            // 异步通知业务层
            getEventLoopGroup().getParallelQueueExecutor()
                    .executeOneTime(() -> getCapabilityModel().onOutOfSyncingWaterMarker(
                            outOfSyncingWaterMarkerRemotingDomains));
        }
        removeIds.forEach(id -> syncingRemotingDomainRepository.remove(id));
        return committedRemotingDomains;
    }

    /**
     * 如果超过 syncing water marker 时间，则 remove 调 syncing 的数据。
     *
     * @param remotingDomainWrapper
     * @return true 表示已经 out of syncing water marker，并且已经从 syncing 列表中 remove。false 表示还没有
     * out of syncing water marker。
     */
    private boolean removeIfOutOfSyncingWaterMarker(
            final RemotingDomainWrapper remotingDomainWrapper) {
        long syncingPendingTime = System.currentTimeMillis()
                - remotingDomainWrapper.getStartSyncingTime();
        if (syncingPendingTime > RemotingManager.getRemotingManager().getRaftConfig()
                .getNodeInformation().getSyncingMaxWaterMarker(
                        ClusterDataSyncManager.MAX_SYNCING_WATER_MARKER)) {
            // clear the syncing remoting domain from the memory
            return true;
        }
        return false;
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
        if (raftConfig.getLeader() == null) {
            return;
        }

        if (!raftConfig.getLeader().identify()
                .equals(raftConfig.getNodeInformation().identify())) {
            return;
        }

        this.sendHeartbeatsIfExist(source);
    }

    @Override
    public void setRaftConfig(WareCoreSwiftConfig raftConfig) {
        this.raftConfig = raftConfig;
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
