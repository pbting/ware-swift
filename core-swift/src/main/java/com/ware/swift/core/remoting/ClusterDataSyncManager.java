package com.ware.swift.core.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.remoting.avalispart.IAvailableCapabilityModel;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.conspart.IConsistenceCapabilityModel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public final class ClusterDataSyncManager implements RemotingInteractiveConstants {

    public static final AtomicLong DATA_SYNCING_REMOTING_DOMAIN_ID = new AtomicLong();
    public static final long MAX_SYNCING_WATER_MARKER = TimeUnit.MINUTES.toMillis(30);
    // 仅仅是一个在数据同步过程中的一个保留字段。
    public static final int DATA_SYNC_STATUS_CODE = 3;
    public static final int DATA_SYNC_STATUS_CODE_SUCCESS = 8200;
    public static final int DATA_SYNC_STATUS_CODE_ERROR = 8503;
    // DATA_SYNC_STATUS_* 是在节点间进行通信时 response 给带上的
    public static final String DATA_SYNC_REASON_PHRASE_MOVED = "MOVED %s";
    public static final String DATA_SYNC_REASON_PHRASE_SUCCESS = "%s SYNCING SUCCESS";
    public static final String DATA_SYNC_REASON_PHRASE_ERROR = "%s SYNCING ERROR[%s]";
    public static final String DATA_STREAM_REPLICATION_REASON_PHRASE_COMPLETE = "replication complete";
    public static final String DATA_STREAM_REPLICATION_REASON_PHRASE_START = "replication starting";

    public static final String DATA_SYNC_EMPTY_COMMITTED_IDS = "EMPTY COMMITTED IDS";
    public static final String DATA_SYNC_STREAM_COMMITTED_TOPIC = "dataSyncStreamCommittedTopic";
    public static final String DATA_SYNC_STREAM_SYNCING_TOPIC = "dataSyncStreamSyncingTopic";

    public static final IClusterSyncCallbackHoot EMPTY_CALLBACK_HOOK = new IClusterSyncCallbackHoot.EmptyClusterSyncCallbackHook();

    private static final AtomicBoolean isStartDataStreamReplication = new AtomicBoolean(false);

    /**
     * @param nodeIdentify
     * @return
     */
    public static String formatMovedResonPhrase(String nodeIdentify) {

        return String.format(DATA_SYNC_REASON_PHRASE_MOVED, nodeIdentify);
    }

    /**
     * @param remotingDomainClass
     * @return
     */
    public static String formatSuccessReasonPhrase(String remotingDomainClass) {

        return String.format(DATA_SYNC_REASON_PHRASE_SUCCESS, remotingDomainClass);
    }

    /**
     * @param errorCard
     * @param errorMessage
     * @return
     */
    public static String formatErrorReasonPhrase(String errorCard,
                                                 String errorMessage) {

        return String.format(DATA_SYNC_REASON_PHRASE_ERROR, errorCard, errorMessage);
    }

    /**
     * @return the zero index meas equest Required and the one index is active channel
     * Acks
     */
    public static int[] buildRequireAcksAndGetActiveChannel() {
        int[] requireAcksAndActiveChannel = new int[2];

        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        int defaultRequestRequiredAcks = remotingManager.getActiveChannelCount() + 1;
        int requestRequiredAcks = remotingManager.getWareSwiftConfig()
                .getNodeInformation().getRequestRequiredAcks(defaultRequestRequiredAcks);
        requireAcksAndActiveChannel[0] = requestRequiredAcks;
        requireAcksAndActiveChannel[1] = defaultRequestRequiredAcks;
        return requireAcksAndActiveChannel;
    }

    public static String generatorDataSyncingId(String localNodeIdentify) {
        String idFormat = "%s.%s.%s";
        return String.format(idFormat, localNodeIdentify,
                DATA_SYNCING_REMOTING_DOMAIN_ID.incrementAndGet(),
                System.currentTimeMillis());
    }

    /**
     * 基于需要同步的数据 new 一个用来两个进程之间需要同步的数据结构{@link InteractivePayload}
     *
     * @param remotingDomain
     * @return
     */
    public static InteractivePayload newSyncInteractivePayload(
            RemotingDomainSupport remotingDomain) {

        return newSyncInteractivePayload(remotingDomain, EMPTY_CALLBACK_HOOK);
    }

    /**
     * 基于需要同步的数据 new 一个用来两个进程之间需要同步的数据结构{@link InteractivePayload}
     *
     * @param remotingDomain
     * @return
     */
    public static InteractivePayload newSyncInteractivePayload(
            RemotingDomainSupport remotingDomain, IClusterSyncCallbackHoot callbackHook) {
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        final WareSwiftConfig wareSwiftConfig = remotingManager.getWareSwiftConfig();
        long term = wareSwiftConfig.getNodeInformation().getTerm();
        if (RemotingManager.isDecenterationRemotingManager()) {
            term = wareSwiftConfig.getNodeInformation().getDecentralizeTerm();
        }

        String source = remotingManager.getChannelIdentify(
                wareSwiftConfig.getNodeInformation().identify());
        String currentDataSyncingId = ClusterDataSyncManager.generatorDataSyncingId(source);
        // 这块构建 Interactive payload 代码是可以优化的。
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSource(source);
        // callback hook 放在下面的前面，是因为以下这些参数的设置是不能被覆盖的。
        builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS,
                remotingDomain.getClass().getName());
        builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE, term + "");
        builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID,
                currentDataSyncingId);
        builder.setEventType(RemotingEventDispatcher.REMOTING_DATA_SYNCING_EVENT_TYPE);
        builder.setPayload(
                ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .encodingResult(remotingDomain)));
        callbackHook.callback(builder);
        return builder.build();
    }

    /**
     * 判断是否已经超过最大的 同步空闲时间。
     * <p>
     * 通常是在 Follower 节点重新连接或者上线时，需要等待一个具体的时间段，然后 Leader 在开始进行 syncing 状态的数据同步。
     * <p>
     * 主要是用来解决重新连接或者上线时，Follower 节点也要开始和 Leader 节点进行同步，这个时候也会将 committed 和 syncing 两个状态
     * <p>
     * 的数据都会同步过来，因此为了和这个阶段的同步进行错峰，增加一个 syncing max idle time 的配置。
     * <p>
     * 如果不错峰，则心跳时会 commit 值为 term 的所有 syncing 数据，但是 Follower 节点没有这个数据，Leader 节点认为已经提交
     * success 了。就会将内存中正在 syncing 状态的数据都会变更为 committed 。但是 Follower 节点没有，并且
     *
     * @param baseTime
     * @param defaultTime 以秒为时间单位。
     */
    public static boolean isOutOfCommittedMaxIdleTime(long baseTime, long defaultTime) {
        IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        long syncingMaxIdleTime = remotingManager.getWareSwiftConfig().getNodeInformation()
                .getCommittedMaxIdleTime(TimeUnit.SECONDS.toMillis(defaultTime));

        return System.currentTimeMillis() - baseTime > syncingMaxIdleTime;
    }

    /**
     * 1. 同步已经 committed 的数据
     * <p>
     * 2. 同步正在同步过程中的数据
     * <p>
     * 此方法的调用一般发生在两个场景:
     * <p>
     * 1. Follower 节点有 offline -> online的状态， 和 Leader 节点重新建立连接。
     * <p>
     * 2. Leader offline ，重新选举。Follower 重新指向新的 Leader，这个时候需要和 Leader
     * 进行一次全量/增量的数据同步。做数据的强一致性保证。
     * <p>
     * 目前实现的是全量
     *
     * @param remotingChannel
     */
    public static void startDataReplication(AbstractRemotingChannel remotingChannel) {
        if (!isStartDataStreamReplication.compareAndSet(false, true)) {
            // 只启动一次即可，去重操作。
            return;
        }

        // 1. 开始同步已经提交的数据
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        builder.setSource(remotingManager.getChannelIdentify(
                remotingManager.getWareSwiftConfig().getNodeInformation().identify()));
        builder.setEventType(
                RemotingEventDispatcher.REMOTING_STREAM_DATA_SYNC_EVENT_TYPE);
        builder.setPayload(ByteString
                .copyFrom(ClusterDataSyncManager.DATA_SYNC_STREAM_COMMITTED_TOPIC
                        .getBytes(CharsetUtil.UTF_8)));
        remotingChannel.requestStream(builder.build()).registryCallback(
                (interactivePayload -> {
                    // 判断是否结束
                    onReSyncCommitted(interactivePayload);
                }));

        // 2.开始同步正在同步中的数据
        builder.setPayload(
                ByteString.copyFrom(ClusterDataSyncManager.DATA_SYNC_STREAM_SYNCING_TOPIC
                        .getBytes(CharsetUtil.UTF_8)));
        remotingChannel.requestStream(builder.build()).registryCallback(
                (interactivePayload -> onReSyncCommitted(interactivePayload)));
    }

    /**
     * syncing 状态的数据重新同步一次，这次直接处理的结果是 committed。
     *
     * @param interactivePayload
     */
    private static void onReSyncCommitted(final InteractivePayload interactivePayload) {
        String remotingDomainClass = interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS);
        try {
            Class clazz = Class.forName(remotingDomainClass);
            RemotingDomainSupport remotingDomain = (RemotingDomainSupport) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(interactivePayload.getPayload().toByteArray(), clazz);

            IRemotingManager remotingManager = RemotingManager.getRemotingManager();
            ICapabilityModel capabilityModel = remotingManager.getCapabilityModel();
            if (capabilityModel instanceof IConsistenceCapabilityModel) {
                IConsistenceCapabilityModel consistenceCapabilityModel = (IConsistenceCapabilityModel) capabilityModel;
                consistenceCapabilityModel.onCommitted(Arrays.asList(remotingDomain));
                remotingManager.getWareSwiftConfig().getNodeInformation().incrementCommittedCount();
            } else if (capabilityModel instanceof IAvailableCapabilityModel) {
                IAvailableCapabilityModel availableCapabilityModel = (IAvailableCapabilityModel) capabilityModel;
                try {
                    availableCapabilityModel.onInboundDataSet(interactivePayload.getSource(), remotingDomain,
                            new HashMap<>());
                } catch (RemotingInteractiveException e) {
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
