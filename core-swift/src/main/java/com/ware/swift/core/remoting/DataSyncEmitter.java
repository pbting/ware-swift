package com.ware.swift.core.remoting;

import com.ware.swift.event.ObjectEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 同步数据发射器。在节点重连时 Leader 节点往新建立连接的节点同步数据时，可以用到这里。
 */
public class DataSyncEmitter {

    private IInteractive interactive;
    private List<RemotingDomainSupport> syncFails = new LinkedList<>();

    private AtomicBoolean finishSwitch = new AtomicBoolean(false);

    public DataSyncEmitter(IInteractive interactive) {
        this.interactive = interactive;
    }

    /**
     * 发射一个数据
     *
     * @param remotingDomain
     */
    public void onEmit(RemotingDomainSupport remotingDomain) {
        boolean isSendSuccess = interactive.sendPayload(
                ClusterDataSyncManager.newSyncInteractivePayload(remotingDomain));
        if (!isSendSuccess) {
            syncFails.add(remotingDomain);
        }
    }

    public void onEmitFinish() {
        if (syncFails.size() > 0) {
            synchronized (syncFails) {
                if (syncFails.isEmpty()) {
                    return;
                }
                List<RemotingDomainSupport> tmpSyncFails = new LinkedList<>();
                tmpSyncFails.addAll(syncFails);
                ObjectEvent objectEvent = new ObjectEvent(interactive, tmpSyncFails,
                        AbstractRemotingManager.SYNC_FAIL_EVENT_TYPE);
                RemotingManager.getRemotingManager().getEventLoopGroup()
                        .notifyListeners(objectEvent);
                syncFails.clear();
            }
        }
    }
}
