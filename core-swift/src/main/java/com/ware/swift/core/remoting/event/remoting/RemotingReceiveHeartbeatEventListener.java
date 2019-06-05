package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 接收 Leader 发送过来的心跳
 */
public abstract class RemotingReceiveHeartbeatEventListener
        extends AbstractRemotingPipelineEventListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RemotingReceiveHeartbeatEventListener.class);

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive interactive = event.getValue();
        final InteractivePayload heartbeatPayload = interactive.getInteractivePayload();
        InteractivePayload interactivePayload = processHeartbeatPayload(heartbeatPayload);
        interactive.sendPayload(interactivePayload);
        return true;
    }

    /**
     * @return
     */
    public String onPrepareCommittedRemotingDomains(InteractivePayload heartbeatPayload) {

        // 半同步状态下的数据处理
        String[] semiSyncingRemotingDomainIds = new String[0];
        if (heartbeatPayload.getHeadersMap().containsKey(ClusterDataSyncManager.HEADER_KEY_SEMI_SYNCING_REMOTING_DOMAIN_ID)) {
            String semiSyncingRemotingIds = heartbeatPayload.getHeadersMap().get(ClusterDataSyncManager.HEADER_KEY_SEMI_SYNCING_REMOTING_DOMAIN_ID);
            semiSyncingRemotingDomainIds = semiSyncingRemotingIds.split(ClusterDataSyncManager.SYNCING_IDS_SEPARATOR);
        }

        long receiveTerm = Long.valueOf(heartbeatPayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE));
        logger.info(" Receive term=" + receiveTerm);
        String syncingDomainIds = RemotingManager.getRemotingManager()
                .getTransactionModel().prepareCommittedRemotingDomains(heartbeatPayload.getSource(),
                        receiveTerm, semiSyncingRemotingDomainIds);

        return syncingDomainIds;
    }

    /**
     * 如果有必要，则启动数据复制
     *
     * @param heartbeatPayload
     */
    public void startDataReplicationIfNecessary(InteractivePayload heartbeatPayload) {
        if (!heartbeatPayload.containsHeaders(ClusterDataSyncManager.HEADER_KEY_DATA_STREAM_REPLICATION)) {
            return;
        }

        boolean isNeedReplication = Boolean.valueOf(heartbeatPayload.getHeadersMap().get(ClusterDataSyncManager.HEADER_KEY_DATA_STREAM_REPLICATION));
        if (isNeedReplication) {
            ClusterDataSyncManager.startDataReplication(RemotingManager.getRemotingManager().getRemotingChannel(heartbeatPayload.getSource()));
        }
    }

    public abstract InteractivePayload processHeartbeatPayload(InteractivePayload heartbeatPayload);
}
