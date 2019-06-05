package com.ware.swift.core.remoting.channel;

import com.ware.swift.core.Identify;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public abstract class AbstractRemotingChannel
        implements Identify, IRequestResponseChannel, IRequestStreamChannel,
        IBiRequestChannel, IFireAndForgetChannel {

    public enum JoinType {
        SYSTEM_STARTUP, NODE_MEET
    }

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(AbstractRemotingChannel.class);

    private ReentrantLock syncingRemotingDomainLock = new ReentrantLock(true);
    /**
     * 用来记录正在同步中的数据，正在同步中的数据，如果是 CP 模型，此时对所有的客户端是不可见的。
     * <p>
     * 等第二阶段 committed 之后才变为可见。sync-ing -> synced
     */
    private ConcurrentHashMap<String, InteractivePayload> syncingRemotingDomainRepository = new ConcurrentHashMap<>();
    /**
     * 最近一次打开在线检测的时间
     */
    private volatile long lastOpenOnlineCheckTime = -1;

    /**
     * 最近一次关闭在线检测的时间
     */
    private volatile long lastCloseOnlineCheckTime = System.currentTimeMillis();

    /**
     *
     */
    private volatile long committedCount = -1;

    /**
     *
     */
    private JoinType joinType = JoinType.SYSTEM_STARTUP;
    /**
     *
     */
    private String addressPort;
    /**
     * 一个具有三个元数据信息的三元组。用 "@" 符隔开。
     * <p>
     * 这三个元数据信息分别是: role@ip:port@cluster_name
     */
    private String identify;


    private AtomicBoolean isOpenOnlineCheck = new AtomicBoolean(false);
    /**
     * 对于通过 node meet 加入进来的节点，是需要进行一次数据同步的。
     * <p>
     * 注意数据同步最好是:
     * <p>
     * Leader-Follower: 由 Leader 节点来通知 Follower 节点。
     * Decentralize: 有当前节点来通知加入的节点
     * </p>
     */
    private AtomicBoolean isStartDataReplication = new AtomicBoolean(false);

    /**
     *
     */
    private AtomicLong lastSendHeartbeatTime = new AtomicLong(-1);

    public AbstractRemotingChannel(String addressPort, JoinType joinType) {
        this.addressPort = addressPort;
        this.joinType = joinType;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public boolean isStartDataReplication() {
        return isStartDataReplication.get();
    }

    public boolean startDataReplication() {

        return isStartDataReplication.compareAndSet(false, true);
    }

    public void closeStartDataReplication() {
        isStartDataReplication.compareAndSet(true, false);
    }

    public String getAddressPort() {
        return addressPort;
    }

    @Override
    public String identify() {
        return this.identify;
    }

    /**
     * format: ${prefix}@${address_port}@${cluster_name}
     *
     * @return
     */
    public void setIdentify(String identify) {
        this.identify = identify;
    }

    /**
     * @return true 表示已经打开，false 表示已经关闭
     */
    public boolean isOpenOnlineCheck() {
        return isOpenOnlineCheck.get();
    }

    public void openOnlineCheckStatus() {
        lastOpenOnlineCheckTime = System.currentTimeMillis();
        isOpenOnlineCheck.set(true);
    }

    public void closeOnlineCheckStatus() {
        if (isOpenOnlineCheck.compareAndSet(true, false)) {
            lastCloseOnlineCheckTime = System.currentTimeMillis();
            log.debug(identify + " is offline "
                    + (System.currentTimeMillis() - lastOpenOnlineCheckTime) + " Ms。");
        }
    }

    /**
     * @return
     */
    public AtomicLong getLastSendHeartbeatTime() {
        return lastSendHeartbeatTime;
    }

    /**
     * @param addressPort
     * @param clusterName
     * @return
     * @throws Exception
     */
    public static AbstractRemotingChannel addNewNode(String addressPort,
                                                     String clusterName, JoinType joinType) {
        AbstractRemotingChannel remotingChannel = RemotingManager.getRemotingManager()
                .getRemotingChannelFactory().newRemotingChannel(addressPort, clusterName, joinType);
        RemotingManager.getRemotingManager().addRemotingChannel(remotingChannel);
        return remotingChannel;
    }

    public long getLastCloseOnlineCheckTime() {
        return lastCloseOnlineCheckTime;
    }

    public long getCommittedCount() {
        return committedCount;
    }

    public void setCommittedCount(long committedCount) {
        this.committedCount = committedCount;
    }

    public int addSyncingRemotingDomain(final InteractivePayload interactivePayload) {

        final String syncingId = interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID);
        syncingRemotingDomainLock.lock();
        try {
            syncingRemotingDomainRepository.put(syncingId, interactivePayload);
        } finally {
            syncingRemotingDomainLock.unlock();
        }

        return syncingRemotingDomainRepository.size();
    }

    public Collection<InteractivePayload> clearAndGetAllSyncingRemotingDomains() {

        LinkedList linkedList = new LinkedList();
        syncingRemotingDomainLock.lock();
        try {
            linkedList.addAll(syncingRemotingDomainRepository.values());
            syncingRemotingDomainRepository.clear();
            return linkedList;
        } finally {
            syncingRemotingDomainLock.unlock();
        }
    }

    public abstract void close();

    public abstract boolean isNetUnavailable(Exception e);

}
