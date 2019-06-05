package com.ware.swift.core;

import com.ware.swift.core.remoting.ClusterDataSyncManager;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class NodeInformation implements Identify {

    public static final String BIND_ADDRESS = "bind.address";
    public static final String BIND_PORT = "bind.port";
    public static final String REQUEST_REQUIRED_ACKS = "request.required.acks";
    public static final String MAX_SYNCING_WATER_MARKER = "max.syncing.water.marker";
    public static final String MAX_COMMITTED_IDLE_TIME = "max.committed.idle.time";
    public static final String EMPTY_VOTE_FOR = "emptyVoteFor";
    public static final String SEND_HEARTBEAT_INTERVAL = "send.heartbeat.interval";
    public static final String WARE_SWIFT_DEBUG = "ware.swift.debug";
    public static final String REPL_BACKLOG_SYNCING_QUEUE_SIZE = "repl.backlog.syncing.queue.size";

    private volatile long voteCount = 0;
    private volatile long term = ClusterDataSyncManager.DEFAULT_SYNCING_TERM;
    private volatile long decentralizeTerm = ClusterDataSyncManager.DEFAULT_SYNCING_TERM;
    private volatile long voteForTime;
    private volatile long hasCommittedCount = 0;
    private AtomicReference<String> atomicVotedFor = new AtomicReference<>(
            EMPTY_VOTE_FOR);
    /**
     * node start in the follower state.
     */
    private volatile NodeState nodeState = NodeState.Follower;

    /**
     * Which cluster does a node belong to?
     */
    private String clusterName = "RAFT_DEFAULT_CLUSTER";
    private String bindAddress;
    private String bindPort;

    /**
     * subjectively down 的投票数。当达到剩下的 Follower 节点半数以上同意，则认为确实是下线了。
     */
    private AtomicLong sdownVoteCount = new AtomicLong();

    private transient Properties config;

    public void setConfig(Properties config) {
        this.config = config;
        this.bindAddress = config.getProperty("bind.address", "0.0.0.0");
        this.bindPort = config.getProperty("bind.port", "19090");
        this.clusterName = config.getProperty("cluster.name", clusterName);
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    public void setNodeState(NodeState nodeState) {
        this.nodeState = nodeState;
    }

    public String getClusterName() {
        return clusterName;
    }

    public InetSocketAddress getInetSocketAddress() {
        String ip = config.getProperty(BIND_ADDRESS, "0.0.0.0");
        int port = Integer.valueOf(config.getProperty(BIND_PORT, "19090"));
        return new InetSocketAddress(ip, port);
    }

    public String getAddressPort() {
        if (this.bindAddress == null && this.bindPort == null) {
            return null;
        }

        return this.bindAddress + ":" + this.bindPort;
    }

    public boolean isDebug() {

        return Boolean.valueOf(config.getProperty(WARE_SWIFT_DEBUG));
    }

    public String getBindAddress() {
        return this.bindAddress;
    }

    public String getBindPort() {
        return this.bindPort;
    }

    public long getVoteCount() {
        return voteCount;
    }

    public synchronized void increVoteCount() {
        this.voteCount++;
        this.voteForTime += System.currentTimeMillis();
    }

    public void setDecentralizeTerm(long decentralizeTerm) {
        this.decentralizeTerm = decentralizeTerm;
    }

    public long getDecentralizeTerm() {
        return decentralizeTerm;
    }

    public long getTerm() {
        return term;
    }

    public synchronized void increTerm() {
        this.term++;
    }

    public String getVotedFor() {
        return atomicVotedFor.get();
    }

    /**
     * 投票的时候，需要保证并发性。因此这里需要处理如果发现已经投过票了，则返回 false.
     *
     * @param votedFor
     * @return
     */
    public boolean setVotedFor(String votedFor) {
        return atomicVotedFor.compareAndSet(EMPTY_VOTE_FOR, votedFor);
    }

    public int getRequestRequiredAcks(int defaultValue) {

        String acks = config.getProperty(REQUEST_REQUIRED_ACKS);

        return StringUtil.isNullOrEmpty(acks) ? defaultValue : Integer.valueOf(acks);
    }

    /**
     * @param defaultValue
     * @return
     */
    public long getSyncingMaxWaterMarker(long defaultValue) {

        String maxSyncingWaterMarker = config.getProperty(MAX_SYNCING_WATER_MARKER);
        return StringUtil.isNullOrEmpty(maxSyncingWaterMarker) ? defaultValue
                : Long.valueOf(maxSyncingWaterMarker);
    }

    public int getReplBacklogSyncingQueueSize(int defaultValue) {
        String replBacklogSyncingQueueSize = config.getProperty(REPL_BACKLOG_SYNCING_QUEUE_SIZE);
        return StringUtil.isNullOrEmpty(replBacklogSyncingQueueSize) ? defaultValue
                : Integer.valueOf(replBacklogSyncingQueueSize);
    }

    /**
     * 即用来表示
     *
     * @param defaultValue
     * @return
     */
    public long getCommittedMaxIdleTime(long defaultValue) {

        String maxCommittedIdleTimes = config.getProperty(MAX_COMMITTED_IDLE_TIME);

        return StringUtil.isNullOrEmpty(maxCommittedIdleTimes) ? defaultValue
                : Long.valueOf(maxCommittedIdleTimes);
    }

    /**
     * 在感知到 leader 之后，做一些基本信息的 update 操作。
     * <p>
     * 注意: 需要更新本 节点的 vote for 信息 set is null。因此这个过程中有可能本节点已经投自己一票了，但是最终没有选举成功。
     * <p>
     * 为下一次选举投票做好准备。
     *
     * @param leader
     */
    public synchronized void updateAfterAwareLeader(NodeInformation leader) {
        this.atomicVotedFor.set(EMPTY_VOTE_FOR);
        this.term = leader.getTerm();
        this.hasCommittedCount = leader.getCommittedCount();
        if (leader.identify().equalsIgnoreCase(this.identify())) {
            this.setNodeState(NodeState.Leader);
        } else {
            this.setNodeState(NodeState.Follower);
        }
    }

    /**
     * 清除之前的一些必要的投票状态信息
     */
    public synchronized void clearVoteStatus() {
        this.atomicVotedFor.set(EMPTY_VOTE_FOR);
        this.voteForTime = 0;
        this.voteCount = 0;
        this.nodeState = NodeState.Follower;
    }

    public AtomicLong getSdownVoteCount() {
        return sdownVoteCount;
    }

    /**
     * 节点在哪个集群。${address:port}@${cluster_name}
     *
     * @return
     */
    @Override
    public String identify() {
        return getAddressPort() + "@" + getClusterName();
    }

    /**
     * 数据 committed 数 + 1 。主要有以下三种场景会触发:
     * <p>
     * 1. Leader 节点主动 committed
     * 2. Follower 节点被动 committed
     * 3. Decentralize 架构下当前节点同步数据时
     * </p>
     *
     * @return
     */
    public long incrementCommittedCount() {
        long tmpHashCommittedCount = hasCommittedCount;
        tmpHashCommittedCount += 1;
        hasCommittedCount = tmpHashCommittedCount;
        return tmpHashCommittedCount;
    }

    public long getCommittedCount() {

        return hasCommittedCount;
    }

    public long getHeartbeatInterval(long defaultValue) {

        return Long.valueOf(config.getProperty(SEND_HEARTBEAT_INTERVAL,
                String.valueOf(defaultValue)));
    }

    @Override
    public String toString() {
        return "NodeInformation{" +
                "voteCount=" + voteCount +
                ", term=" + term +
                ", decentralizeTerm=" + decentralizeTerm +
                ", voteForTime=" + voteForTime +
                ", hasCommittedCount=" + hasCommittedCount +
                ", atomicVotedFor=" + atomicVotedFor +
                ", nodeState=" + nodeState +
                ", clusterName='" + clusterName + '\'' +
                ", bindAddress='" + bindAddress + '\'' +
                ", bindPort='" + bindPort + '\'' +
                ", sdownVoteCount=" + sdownVoteCount +
                ", config=" + config +
                '}';
    }
}
