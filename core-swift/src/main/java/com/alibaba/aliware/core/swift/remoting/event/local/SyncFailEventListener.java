package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingDomain;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class SyncFailEventListener
        extends AbstractLocalPipelineEventListener<List<RemotingDomain>> {

    public SyncFailEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<List<RemotingDomain>> event, int listenerIndex) {

        IInteractive source = (IInteractive) event.getSource();
        List<RemotingDomain> fails = event.getValue();
        System.err.println("\t检测到同步失败数:" + fails.size());
        List<RemotingDomain> collectorFails = new LinkedList<>();
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
