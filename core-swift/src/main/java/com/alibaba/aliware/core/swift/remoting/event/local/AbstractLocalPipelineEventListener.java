package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.grpc.swift.event.swift.object.pipeline.IPipelineEventListener;

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
