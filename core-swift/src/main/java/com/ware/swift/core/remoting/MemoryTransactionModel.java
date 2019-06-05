/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ware.swift.core.remoting;

import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author pbting
 * @date 2019-05-24 12:24 AM
 */
public class MemoryTransactionModel implements ITransactionModel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MemoryTransactionModel.class);
    /**
     * 用来记录正在同步中的数据，正在同步中的数据，如果是 CP 模型，此时对所有的客户端是不可见的。
     * <p>
     * 等第二阶段 committed 之后才变为可见。sync-ing -> synced
     * <p>
     * 主要使用来处理 Leader-Follower 架构下的 syncing 状态的数据存储。
     */
    private ConcurrentHashMap<String, RemotingDomainWrapper> syncingRemotingDomainRepository = new ConcurrentHashMap<>();

    /**
     * 节点间进行数据同步时的缓冲队列。
     */
    private Queue<RemotingDomainSupport> replBacklogSyncingQueue = new ConcurrentLinkedDeque();


    private AtomicBoolean isReplBacklogSyncingOutput = new AtomicBoolean();

    /**
     * 记录当前正在进行数据流复制的线程数
     */
    private AtomicInteger dataStreamReplicationCount = new AtomicInteger();

    /**
     * 去中心的架构下，用来记录当前是否开启数据写入时是否需要写入 backlog 队列
     */
    private AtomicBoolean decentralizeBacklogSwitch = new AtomicBoolean();
    /**
     * 记录那些处于半同步状态下的数据。主要是处理部分节点同步成功，部分节点同步失败时的情况，需要同步已经同步成功的节点无需提交。
     */
    private ConcurrentHashMap<String, RemotingDomainWrapper> semiSyncingRemotingDomainRepository = new ConcurrentHashMap<>();
    /**
     *
     */
    private Set<String> collectorCommittedIds = new ConcurrentSkipListSet<>();

    /**
     * @param remotingDomainWrapper
     * @return
     */
    @Override
    public int addSyncingRemotingDomain(
            final RemotingDomainWrapper remotingDomainWrapper) {
        syncingRemotingDomainRepository.put(remotingDomainWrapper.getSyncingId(),
                remotingDomainWrapper);
        return syncingRemotingDomainRepository.size();
    }

    @Override
    public int addSemiSyncingRemotingDomain(RemotingDomainWrapper remotingDomainWrapper) {
        semiSyncingRemotingDomainRepository.put(remotingDomainWrapper.getSyncingId(), remotingDomainWrapper);
        return semiSyncingRemotingDomainRepository.size();
    }

    @Override
    public Collection<RemotingDomainWrapper> getAndClearSemiSyncingRemotingDomainIds() {
        Collection<RemotingDomainWrapper> remotingDomainWrappers = new LinkedList<>();

        if (semiSyncingRemotingDomainRepository.size() > 0) {
            synchronized (semiSyncingRemotingDomainRepository) {
                remotingDomainWrappers.addAll(semiSyncingRemotingDomainRepository.values());
                semiSyncingRemotingDomainRepository.clear();
            }
        }

        return remotingDomainWrappers;
    }

    @Override
    public void visitSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor) {
        syncingRemotingDomainRepository.values().forEach(visitor);
    }

    @Override
    public void processReplBacklogSyncingQueue() {
        if (replBacklogSyncingQueue.isEmpty()) {
            return;
        }

        if (isReplBacklogSyncingOutput.compareAndSet(false, true)) {
            System.out.println("\t replication backlog size =" + replBacklogSyncingQueue.size());
            try {
                final ICapabilityModel capabilityModel = RemotingManager.getRemotingManager().getCapabilityModel();
                for (RemotingDomainSupport remotingDomainSupport : replBacklogSyncingQueue) {
                    capabilityModel.onOutboundDataSet(remotingDomainSupport, new OutboundCallback() {
                        @Override
                        public void onSyncingSuccess(Set<String> channelIdentifies) {
                            logger.info("replication backlog remoting domain out bound success {}", channelIdentifies.toString());
                        }
                    });
                }
                replBacklogSyncingQueue.clear();
            } finally {
                isReplBacklogSyncingOutput.compareAndSet(true, false);
            }
        }
    }

    @Override
    public boolean isInDataStreamReplication() {
        return dataStreamReplicationCount.get() != 0;
    }

    @Override
    public int getDataStreamReplicationCount() {
        return dataStreamReplicationCount.get();
    }

    @Override
    public int dataStreamReplicationIncre() {
        return dataStreamReplicationCount.incrementAndGet();
    }

    @Override
    public int dataStreamReplicationDecre() {

        return dataStreamReplicationCount.decrementAndGet();
    }

    @Override
    public boolean getDecentralizeBacklogSwitch() {
        return decentralizeBacklogSwitch.get();
    }

    @Override
    public boolean startDecentralizeBacklogSwitch() {
        return decentralizeBacklogSwitch.compareAndSet(false, true);
    }

    @Override
    public boolean closeDecentralizeBacklogSwitch() {
        return decentralizeBacklogSwitch.compareAndSet(true, false);
    }

    @Override
    public void setDataStreamReplicationCount(int count) {

        dataStreamReplicationCount.set(count);
    }

    @Override
    public void visitSemiSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor) {
        semiSyncingRemotingDomainRepository.values().forEach(visitor);
    }

    @Override
    public void removeSyncingRemotingDomain(RemotingDomainWrapper remotingDomainWrapper) {
        syncingRemotingDomainRepository.remove(remotingDomainWrapper.getSyncingId());
    }

    @Override
    public boolean addReplBacklogSyncingQueue(RemotingDomainSupport remotingDomainWrapper) {

        int size = replBacklogSyncingQueue.size();
        int maxSize = RemotingManager.getRemotingManager().getWareSwiftConfig().getNodeInformation().getReplBacklogSyncingQueueSize(10240);
        if (size > maxSize) {
            return false;
        }
        System.out.println("\t\t addReplBacklogSyncingQueue size=" + replBacklogSyncingQueue.size());
        return replBacklogSyncingQueue.offer(remotingDomainWrapper);
    }

    /**
     * 对 committed 计数增一，同时收集已经 committed 成功的 remoting domain,和 out of syncing water marker
     * <p>
     * 主动 committed
     *
     * @param remotingChannel
     */
    @Override
    public Collection<RemotingDomainSupport> getCommittedRemotingDomains(
            Collection<String> committedIds, AbstractRemotingChannel remotingChannel) {
        collectorCommittedIds.addAll(committedIds);
        // check
        int size = syncingRemotingDomainRepository.size();
        System.err.println("\t collector size=" + collectorCommittedIds.size() + ";\t syncing data set size=" + syncingRemotingDomainRepository.size());
        if (size == 0) {
            return Collections.emptyList();
        }
        final Collection<RemotingDomainSupport> committedRemotingDomain = new LinkedList<>();
        final Collection<RemotingDomainSupport> outOfSyncingWaterMarkerRemotingDomain = new LinkedList<>();
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        int activeChannel = remotingManager.getActiveChannelCount();
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
            if (!collectorCommittedIds
                    .contains(remotingDomainWrapper.getSyncingId())) {

                if (removeIfOutOfSyncingWaterMarker(remotingDomainWrapper)) {
                    long term = RemotingManager
                            .getRemotingManager().getWareSwiftConfig().getLeader().getTerm();

                    if (RemotingManager.isDecenterationRemotingManager()) {
                        term = RemotingManager
                                .getRemotingManager().getWareSwiftConfig().getNodeInformation().getDecentralizeTerm();

                    }
                    if (remotingDomainWrapper.getSyncingTerm() == term) {
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
                    .getRequestRequireAcks() - 1) {

                if (remotingDomainWrapper.committed()) {
                    committedRemotingDomain
                            .add(remotingDomainWrapper.getRemotingDomain());
                    remotingManager.getWareSwiftConfig()
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
                RemotingDomainWrapper remotingDomainWrapper = syncingRemotingDomainRepository.remove(in);
                if (remotingDomainWrapper != null) {
                    remotingDomainWrapper.clear();
                }
                collectorCommittedIds.remove(in);
            });
        }

        if (outOfSyncingWaterMarkerRemotingDomain.size() > 0) {
            remotingManager.getEventLoopGroup().getParallelQueueExecutor()
                    .executeOneTime(() -> remotingManager.getCapabilityModel().onOutOfSyncingWaterMarker(
                            outOfSyncingWaterMarkerRemotingDomain));
        }

        return committedRemotingDomain;
    }

    /**
     * 这个方法通常是 Leader 定时发送心跳时来调用的。Leader 进行数据同步时，需要额外判断 term 的值吗？
     *
     * @param channelSource
     * @param term
     * @param semiSyncingRemotingDomainIds
     * @return
     */
    @Override
    public String prepareCommittedRemotingDomains(String channelSource, long term, String... semiSyncingRemotingDomainIds) {
        int size = syncingRemotingDomainRepository.size();

        if (size == 0) {
            return ClusterDataSyncManager.DATA_SYNC_EMPTY_COMMITTED_IDS;
        }

        //1. 先移除掉处于半同步状态下的数据。
        for (String remotingDomainId : semiSyncingRemotingDomainIds) {
            syncingRemotingDomainRepository.remove(remotingDomainId);
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

        if (stringBuilder.length() > 0) {
            stringBuilder.substring(0, stringBuilder.length() - 1);
        }
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

    @Override
    public Collection<RemotingDomainWrapper> getAndClearSyncingRemotingDomainIds() {
        Collection<RemotingDomainWrapper> remotingDomainWrappers = new LinkedList<>();
        synchronized (syncingRemotingDomainRepository) {
            remotingDomainWrappers.addAll(syncingRemotingDomainRepository.values());
            syncingRemotingDomainRepository.clear();
        }
        return remotingDomainWrappers;
    }

    /**
     * 被动 committed。
     *
     * @param term 当前 node 收到需要 committed  的 term 值
     * @return
     */
    @Override
    public Collection<RemotingDomainSupport> committedSyncingDomains(long term) {

        List<RemotingDomainSupport> committedRemotingDomains = new LinkedList<>();
        final List<RemotingDomainSupport> outOfSyncingWaterMarkerRemotingDomains = new LinkedList<>();
        List<String> removeIds = new LinkedList<>();
        for (RemotingDomainWrapper remotingDomainWrapper : syncingRemotingDomainRepository
                .values()) {
            /**
             * 这里需要注意：
             * Leader-Follower 架构下，每个节点的 term 值是一致的，并且只接受 Leader 发送过来的 committed 信息，所以没有问题。
             *
             * Decentralize 架构下，如果每个节点的 term 值一致，就会将本节点正在同步列表中的数据给 remove 调，从而导致数据的不一致性。
             * 这里的原则是只 committed 目标 term 的值。因此 Decentralize 给每个节点分配全局唯一不冲突的 term 即可。
             */
            if (remotingDomainWrapper.getSyncingTerm() == term) {
                if (remotingDomainWrapper.getHasCommittedChannelSize() > 0) {
                    committedRemotingDomains
                            .add(remotingDomainWrapper.getRemotingDomain());
                    RemotingManager.getRemotingManager().getWareSwiftConfig()
                            .getNodeInformation().incrementCommittedCount();
                    removeIds.add(remotingDomainWrapper.getSyncingId());
                }
            } else if (removeIfOutOfSyncingWaterMarker(remotingDomainWrapper)) {
                removeIds.add(remotingDomainWrapper.getSyncingId());
                outOfSyncingWaterMarkerRemotingDomains
                        .add(remotingDomainWrapper.getRemotingDomain());
            }
        }
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        if (outOfSyncingWaterMarkerRemotingDomains.size() > 0) {
            // 异步通知业务层
            remotingManager.getEventLoopGroup().getParallelQueueExecutor()
                    .executeOneTime(() -> remotingManager.getCapabilityModel().onOutOfSyncingWaterMarker(
                            outOfSyncingWaterMarkerRemotingDomains));
        }
        removeIds.forEach(id -> syncingRemotingDomainRepository.remove(id));
        System.err.println("\t syncing data set size=" + syncingRemotingDomainRepository.size());
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
        if (syncingPendingTime > RemotingManager.getRemotingManager().getWareSwiftConfig()
                .getNodeInformation().getSyncingMaxWaterMarker(
                        ClusterDataSyncManager.MAX_SYNCING_WATER_MARKER)) {
            // clear the syncing remoting domain from the memory
            return true;
        }
        return false;
    }
}