package com.ware.swift.core.remoting;

import com.ware.swift.event.ICallbackHook;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class RemotingDomainWrapper {

    public static final byte OUTBOUND_APPLY = 1 << 1;
    public static final byte INBOUND_APPLY = 1 << 2;
    /**
     * 数据同步时所携带的 term.
     */
    private long syncingTerm;

    /**
     *
     */
    private long startSyncingTime;
    /**
     *
     */
    private String syncingId;
    /**
     * 当前正在同步的数据
     */
    private RemotingDomainSupport remotingDomain;

    /**
     * 用来记录当前这个实例已经同步成功的 remoting channel.
     */
    private volatile Set<String> committedRemotingChannels = new ConcurrentSkipListSet<>();

    /**
     * 记录当前已经被同步成功的节点。
     */
    private Set<String> syncingSuccessChannels;
    /**
     * 需要被确认的 ACK 数.达到指定的 ACKS，此数据就可以对所有的客户端可见。
     */
    private int requestRequireAcks;

    private byte apply;
    /**
     * 标识当前这个数据是否已经 committed。
     */
    private AtomicBoolean hasCommitted = new AtomicBoolean(false);

    /**
     * 标识当前这个提交是否是在进行 Follower 重同步时进行提交的。
     */
    private AtomicBoolean isReSyncCommitted = new AtomicBoolean(false);

    /**
     * 标识当前这个数据是远程哪一个节点同步过来的
     */
    private String remotingSource;

    public RemotingDomainWrapper(RemotingDomainSupport remotingDomain, int requestRequireAcks,
                                 String syncingId, long syncingTerm, byte apply) {
        this.remotingDomain = remotingDomain;
        this.requestRequireAcks = requestRequireAcks;
        this.syncingId = syncingId;
        this.syncingTerm = syncingTerm;
        this.startSyncingTime = System.currentTimeMillis();
        this.apply |= apply;
    }

    public void setRemotingSource(String remotingSource) {
        this.remotingSource = remotingSource;
    }

    public String getRemotingSource() {
        return remotingSource;
    }

    public void setSyncingSuccessChannels(Set<String> syncingSuccessChannels) {
        this.syncingSuccessChannels = syncingSuccessChannels;
    }

    public boolean committed() {

        return hasCommitted.compareAndSet(false, true);
    }

    public RemotingDomainSupport getRemotingDomain() {
        return remotingDomain;
    }

    public void setRemotingDomain(RemotingDomainSupport remotingDomain) {
        this.remotingDomain = remotingDomain;
    }

    public Set<String> getCommittedRemotingChannels() {
        return Collections.unmodifiableSet(committedRemotingChannels);
    }

    public int getHasCommittedChannelSize() {
        return committedRemotingChannels.size();
    }

    public boolean hasCommitted(String committedRemotingChannel) {
        return committedRemotingChannels.contains(committedRemotingChannel);
    }

    /**
     * 返回 committed 成功后的 size
     *
     * @param committedRemotingChannel
     * @return
     */
    public int addCommittedRemotingChannel(String committedRemotingChannel) {
        this.committedRemotingChannels.add(committedRemotingChannel);
        return this.committedRemotingChannels.size();
    }

    /**
     * 返回 committed 成功后的 size
     *
     * @param committedRemotingChannel
     * @return
     */
    public int addCommittedRemotingChannel(String committedRemotingChannel, ICallbackHook callbackHook) {
        boolean isAddSuccess = this.committedRemotingChannels.add(committedRemotingChannel);
        callbackHook.callback(isAddSuccess);
        return this.committedRemotingChannels.size();
    }

    public int getRequestRequireAcks() {
        return requestRequireAcks;
    }

    public String getSyncingId() {
        return syncingId;
    }

    public long getStartSyncingTime() {
        return startSyncingTime;
    }

    public long getSyncingTerm() {
        return syncingTerm;
    }

    public boolean isReSyncCommitted() {
        return isReSyncCommitted.get();
    }

    public void setReSyncCommitted() {
        this.isReSyncCommitted.set(true);
    }

    public void clear() {
        syncingSuccessChannels.clear();
        committedRemotingChannels.clear();
    }

    public boolean isInboundApply() {

        return (apply & INBOUND_APPLY) == INBOUND_APPLY;
    }

    public boolean isOutbound() {

        return (apply & OUTBOUND_APPLY) == OUTBOUND_APPLY;
    }
}
