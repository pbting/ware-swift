package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.remoting.IRemotingManager;

import com.ware.swift.event.object.pipeline.IPipelineEventListener;

/**
 * @param <V>
 */
public abstract class AbstractLocalPipelineEventListener<V>
        implements IPipelineEventListener<V> {

    protected IRemotingManager remotingManager;

    public AbstractLocalPipelineEventListener(IRemotingManager remotingManager) {
        this.remotingManager = remotingManager;
    }
}
