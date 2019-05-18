package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 同步数据发射器。在节点重连时 Leader 节点往新建立连接的节点同步数据时，可以用到这里。
 */
public class DataSyncEmitter {

    private IInteractive interactive;
    private AtomicLong syncDataSize = new AtomicLong();
    private List<RemotingDomain> syncFails = new LinkedList<>();

    public DataSyncEmitter(IInteractive interactive) {
        this.interactive = interactive;
    }

    /**
     * 发射一个数据
     *
     * @param remotingDomain
     */
    public void onEmit(RemotingDomain remotingDomain) {
        boolean isSendSuccess = interactive.sendPayload(
                ClusterDataSyncManager.newSyncInteractivePayload(remotingDomain));
        if (!isSendSuccess) {
            syncFails.add(remotingDomain);
        }
        System.err.println("\t Leader send syncing size=>"
                + syncDataSize.incrementAndGet() + "; send success:" + isSendSuccess);
    }

    public void onEmitFinish() {
        if (syncFails.size() > 0) {
            ObjectEvent objectEvent = new ObjectEvent(interactive, syncFails,
                    AbstractRemotingManager.SYNC_FAIL_EVENT_TYPE);
            RemotingManager.getRemotingManager().getEventLoopGroup()
                    .notifyListeners(objectEvent);
        }
    }
}
