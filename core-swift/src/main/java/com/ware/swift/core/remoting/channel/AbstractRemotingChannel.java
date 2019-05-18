package com.ware.swift.core.remoting.channel;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import com.ware.swift.core.Information;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.proto.InteractivePayload;
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
        implements Information, IRequestResponseChannel, IRequestStreamChannel,
        IBiRequestChannel, IFireAndForgetChannel {

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

    private String addressPort;
    /**
     * 一个具有三个元数据信息的三元组。用 "@" 符隔开。
     * <p>
     * 这三个元数据信息分别是: role@ip:port@cluster_name
     */
    private String identify;
    private AtomicBoolean isOpenOnlineCheck = new AtomicBoolean(false);
    private AtomicLong lastSendHeartbeatTime = new AtomicLong(-1);

    public AbstractRemotingChannel(String addressPort) {
        this.addressPort = addressPort;
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
                                                     String clusterName) {
        AbstractRemotingChannel remotingChannel = RemotingManager.getRemotingManager()
                .getRemotingChannelFactory().newRemotingChannel(addressPort, clusterName);
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

    public int addSyncingRemotingDoamin(final InteractivePayload interactivePayload) {

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

    public abstract boolean isNetUnavailable(Exception e);

}
