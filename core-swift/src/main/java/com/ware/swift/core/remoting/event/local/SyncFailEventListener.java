package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingDomainSupport;
import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.event.ObjectEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class SyncFailEventListener
        extends AbstractLocalPipelineEventListener<List<RemotingDomainSupport>> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SyncFailEventListener.class);
    public SyncFailEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<List<RemotingDomainSupport>> event, int listenerIndex) {

        IInteractive source = (IInteractive) event.getSource();
        List<RemotingDomainSupport> fails = event.getValue();
        logger.info("\t检测到同步失败数:" + fails.size());
        List<RemotingDomainSupport> collectorFails = new LinkedList<>();
        fails.forEach(remotingDomain -> {
            if (!source.sendPayload(
                    ClusterDataSyncManager.newSyncInteractivePayload(remotingDomain))) {
                collectorFails.add(remotingDomain);
            }
        });

        if (collectorFails.size() > 0) {
            event.setValue(collectorFails);
        }
        // is empty 全部同步成功，退出，否则继续下一次同步。
        return collectorFails.isEmpty();
    }
}
