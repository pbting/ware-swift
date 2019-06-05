package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.*;
import com.ware.swift.event.ICallbackHook;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;

import java.util.concurrent.CountDownLatch;

/**
 * 其他节点触发需要从当前这个节点进行数据同步的处理入口。
 */
public class RemotingDataStreamSyncEventListener
        extends AbstractRemotingPipelineEventListener {

    private CountDownLatch countDownLatch = new CountDownLatch(2);

    @Override
    public boolean onEvent(final ObjectEvent<IInteractive> event, int listenerIndex) {
        InteractivePayload payload = event.getValue().getInteractivePayload();
        String syncTopic = payload.getPayload().toStringUtf8();
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        if (ClusterDataSyncManager.DATA_SYNC_STREAM_COMMITTED_TOPIC
                .indexOf(syncTopic) != -1) {
            // 说明同步的是已经 committed 的数据。
            remotingManager.getEventLoopGroup()
                    .getParallelQueueExecutor().executeOneTime(() -> {
                remotingManager.getTransactionModel().dataStreamReplicationIncre();
                DataSyncEmitter dataSyncEmitter = new DataSyncEmitter(
                        event.getValue());
                try {
                    RemotingManager.getRemotingManager().getCapabilityModel()
                            .onDataStreamReplication(dataSyncEmitter);
                } finally {
                    dataSyncEmitter.onEmitFinish();
                    remotingManager.getTransactionModel().dataStreamReplicationDecre();
                    countDownLatch.countDown();
                    if (countDownLatch.getCount() == 0) {
                        processFinish(event.getValue().getInteractivePayload().getSource());
                    }
                }
            });
        } else if (ClusterDataSyncManager.DATA_SYNC_STREAM_SYNCING_TOPIC
                .indexOf(syncTopic) != -1) {
            // 说明同步的是正在同步中的数据。正在同步中的数据，框架自己封装。
            startSyncingDataReplication(event.getValue());
        }
        return true;
    }

    /**
     * 开始对正在是 同步中的数据状态(syncing)的数据进行同步。
     */
    public void startSyncingDataReplication(final IInteractive interactive) {
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        remotingManager.getEventLoopGroup()
                .getParallelQueueExecutor().executeOneTime(() -> {
            final String channelSource = interactive.getInteractivePayload()
                    .getSource();
            ITransactionModel transactionModel = RemotingManager.getRemotingManager()
                    .getTransactionModel();
            transactionModel.dataStreamReplicationIncre();
            try {
                transactionModel.visitSyncingRemotingDomain(remotingDomainWrapper -> {
                    ICallbackHook callbackHook = new ICallbackHook() {
                        @Override
                        public void callback(Object target) {
                            boolean isAddSuccess = (Boolean) target;
                            if (isAddSuccess) {
                                remotingDomainWrapper.setReSyncCommitted();
                                try {
                                    interactive.sendPayload(ClusterDataSyncManager
                                            .newSyncInteractivePayload(remotingDomainWrapper
                                                    .getRemotingDomain()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    remotingDomainWrapper
                            .addCommittedRemotingChannel(channelSource, callbackHook);
                });
            } finally {
                transactionModel.dataStreamReplicationDecre();
                countDownLatch.countDown();
                if (countDownLatch.getCount() == 0) {
                    processFinish(interactive.getInteractivePayload().getSource());
                }
            }
        });
    }

    private void processFinish(final String replicationSource) {
        System.err.println("\t" + replicationSource + " replication finish.");
        // 同时由当前这个节点通知其他节点进入数据写入 backlog queue 中
        final IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        final ByteString payload = ByteString.copyFrom(
                ClusterDataSyncManager.DATA_STREAM_REPLICATION_REASON_PHRASE_COMPLETE.getBytes(CharsetUtil.UTF_8));
        final String source = remotingManager.getChannelIdentify(remotingManager.getWareSwiftConfig().getNodeInformation().identify());
        remotingManager.getRemotingChannels().forEach(remotingChannelEle -> {
            if (remotingChannelEle.isOpenOnlineCheck()) {
                return;
            }
            try {
                remotingChannelEle.requestResponse(
                        InteractivePayload.newBuilder().setSource(source)
                                .setSink(remotingChannelEle.identify())
                                .setEventType(RemotingEventDispatcher.REMOTING_CONTROLLER_BACKLOG_QUEUE_EVENT_TYPE)
                                .setPayload(payload).build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 准备进入下一次的数据复制状态。
        countDownLatch = new CountDownLatch(2);
    }
}
