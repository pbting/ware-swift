package com.ware.swift.core.remoting.avalispart;

import com.ware.swift.core.WareCoreSwiftExceptionCode;
import com.ware.swift.core.remoting.OutboundCallback;
import com.ware.swift.core.remoting.RemotingDomain;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Set;
import java.util.TreeSet;

/**
 * 因为是 AP 能力模型，因此有数据的话，就广播给其他节点，如果被广播的节点当前这个连接有问题，将会在下一个心跳给广播出去。
 * <p>
 * 只要当前这个节点处理成功，就立刻会给客户端返回 success。然后再做集群间的数据同步。
 */
public abstract class AbstractAvailableCapabilityModel
        implements IAvailableCapabilityModel {

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(AbstractAvailableCapabilityModel.class);

    /**
     * 上层业务代码决定当前节点是否需要将数据同步给其他节点。
     * <p>
     * 通常来将，触发这里的情况有两个：
     * <p>
     * 1. 上层业务代码已经判断了当前这个节点是 master，需要往其他 Follower 节点进行数据的同步
     * <p>
     * 2. 上层业务代码再去中心化的情况下，需要将数据同步给其他 n-1 个节点
     * <p>
     * 因此到这里的实现因为是 AP 模型，只关注将数据发给其他 n-1 个节点接口。
     *
     * @param remotingDomain
     * @return
     */
    @Override
    public void onOutboundDataSet(final RemotingDomain remotingDomain,
                                  OutboundCallback callback) {
        RemotingManager.getRemotingManager().getRaftConfig().getNodeInformation()
                .incrementCommittedCount();
        final InteractivePayload interactivePayload = ClusterDataSyncManager
                .newSyncInteractivePayload(remotingDomain);
        final Set<String> syncingSuccessChannels = new TreeSet<>();
        final Set<String> syncingFailedChannels = new TreeSet<>();
        RemotingManager.getRemotingManager().getRemotingChannels()
                .forEach(abstractRemotingChannel -> {
                    // 支持同步发呢，还是异步发呢。小集群，同步发，速度不会很慢。
                    try {
                        if (!abstractRemotingChannel.isOpenOnlineCheck()) {
                            abstractRemotingChannel.requestResponse(interactivePayload);
                            syncingSuccessChannels
                                    .add(abstractRemotingChannel.identify());
                        }
                    } catch (Exception e) {
                        logger.error(WareCoreSwiftExceptionCode.formatExceptionMessage(
                                WareCoreSwiftExceptionCode.REMOTING_OUTBOUND_DATASET_ERROR_CODE,
                                e.getMessage()));
                        abstractRemotingChannel
                                .addSyncingRemotingDoamin(interactivePayload);
                        syncingFailedChannels.add(abstractRemotingChannel.identify());
                    }
                });
        if (syncingFailedChannels.isEmpty()) {
            callback.onSyncingSuccess(syncingSuccessChannels);
        } else {
            callback.onSyncingFailed(syncingSuccessChannels, syncingFailedChannels);
        }
    }
}
