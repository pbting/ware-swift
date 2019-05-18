package com.alibaba.aliware.core.swift.remoting;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 */
public class RemotingDomainWrapper {

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
	private RemotingDomain remotingDomain;

	/**
	 * 用来记录当前这个实例已经同步成功的 remoting channel.
	 */
	private volatile Set<String> committedRemotingChannels = new ConcurrentSkipListSet<>();

	/**
	 * 需要被确认的 ACK 数.达到指定的 ACKS，此数据就可以对所有的客户端可见。
	 */
	private int requestRequireAcks;

	/**
	 * 标识当前这个数据是否已经 committed。
	 */
	private AtomicBoolean hasCommitted = new AtomicBoolean(false);

	/**
	 * 标识当前这个提交是否是在进行 Follower 重同步时进行提交的。
	 */
	private AtomicBoolean isReSyncCommitted = new AtomicBoolean(false);

	public RemotingDomainWrapper(RemotingDomain remotingDomain, int requestRequireAcks,
			String syncingId, long syncingTerm) {
		this.remotingDomain = remotingDomain;
		this.requestRequireAcks = requestRequireAcks;
		this.syncingId = syncingId;
		this.syncingTerm = syncingTerm;
		this.startSyncingTime = System.currentTimeMillis();
	}

	public boolean committed() {

		return hasCommitted.compareAndSet(false, true);
	}

	public RemotingDomain getRemotingDomain() {
		return remotingDomain;
	}

	public void setRemotingDomain(RemotingDomain remotingDomain) {
		this.remotingDomain = remotingDomain;
	}

	public Set<String> getCommittedRemotingChannels() {
		return Collections.unmodifiableSet(committedRemotingChannels);
	}

	public int getHasCommittedChannelSize() {
		return committedRemotingChannels.size();
	}

	/**
	 * 返回 committed 成功后的 size
	 * @param committedRemotingChannel
	 * @return
	 */
	public int addCommittedRemotingChannel(String committedRemotingChannel) {
		this.committedRemotingChannels.add(committedRemotingChannel);
		return this.committedRemotingChannels.size();
	}

	public int getRequestRequireAcks() {
		return requestRequireAcks;
	}

	public void setRequestRequireAcks(int requestRequireAcks) {
		this.requestRequireAcks = requestRequireAcks;
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
}
