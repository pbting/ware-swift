package com.alibaba.aliware.grpc.swift.event.swift.loop;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;

/**
 * @param <V>
 */
public class DefaultEventLoopGroup<V> extends AbstractAsyncEventLoopGroup<V> {

    public DefaultEventLoopGroup(IParallelQueueExecutor executor, boolean isOptimism) {
        super(executor, isOptimism);
    }

    public DefaultEventLoopGroup(IParallelQueueExecutor executor,
                                 long schedulerInterval) {
        super(executor, schedulerInterval);
    }

    public DefaultEventLoopGroup(String executorName, boolean isOptimism) {
        super(executorName, isOptimism);
    }

    public DefaultEventLoopGroup(String executorName, long schedulerInterval) {
        super(executorName, schedulerInterval);
    }

    @Override
    public void attachListener() {
        // nothing to do
    }

}
