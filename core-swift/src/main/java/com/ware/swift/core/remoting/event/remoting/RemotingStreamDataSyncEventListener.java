package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.DataSyncEmitter;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;

/**
 * 其他节点触发需要从当前这个节点进行数据同步的处理入口。
 */
public class RemotingStreamDataSyncEventListener
        extends AbstractRemotingPipelineEventListener {

    @Override
    public boolean onEvent(final ObjectEvent<IInteractive> event, int listenerIndex) {
        InteractivePayload payload = event.getValue().getInteractivePayload();
        String syncTopic = payload.getPayload().toStringUtf8();

        if (ClusterDataSyncManager.DATA_SYNC_STREAM_COMMITTED_TOPIC
                .indexOf(syncTopic) != -1) {
            // 说明同步的是已经 committed 的数据。
            RemotingManager.getRemotingManager().getEventLoopGroup()
                    .getParallelQueueExecutor().executeOneTime(() -> {
                DataSyncEmitter dataSyncEmitter = new DataSyncEmitter(
                        event.getValue());
                RemotingManager.getRemotingManager().getCapabilityModel()
                        .onDataStreamSyncing(dataSyncEmitter);
            });
        } else if (ClusterDataSyncManager.DATA_SYNC_STREAM_SYNCING_TOPIC
                .indexOf(syncTopic) != -1) {
            // 说明同步的是正在同步中的数据。正在同步中的数据，框架自己封装。
            startSyncingDataSync(event.getValue());
        }
        return true;
    }

    /**
     * 开始对正在是 同步中的数据状态(syncing)的数据进行同步。
     */
    public void startSyncingDataSync(final IInteractive interactive) {
        RemotingManager.getRemotingManager().getEventLoopGroup()
                .getParallelQueueExecutor().executeOneTime(() -> {
            final String channelSource = interactive.getInteractivePayload()
                    .getSource();
            RemotingManager.getRemotingManager()
                    .visiterSyncingRemotingDomain(remotingDomainWrapper -> {
                        remotingDomainWrapper
                                .addCommittedRemotingChannel(channelSource);
                        remotingDomainWrapper.setReSyncCommitted();
                        interactive.sendPayload(ClusterDataSyncManager
                                .newSyncInteractivePayload(remotingDomainWrapper
                                        .getRemotingDomain()));
                    });
        });
    }
}
