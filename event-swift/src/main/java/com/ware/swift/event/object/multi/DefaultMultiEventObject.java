package com.ware.swift.event.object.multi;

import com.ware.swift.event.parallel.IParallelQueueExecutor;

/**
 * @param <V>
 */
public class DefaultMultiEventObject<V> extends AbstractMultiEventObject<V> {

    public DefaultMultiEventObject(IParallelQueueExecutor superFastParallelQueueExecutor,
                                   boolean isOptimism) {
        super(superFastParallelQueueExecutor, isOptimism);
    }

    public DefaultMultiEventObject(
            IParallelQueueExecutor superFastParallelQueueExecutor) {
        super(superFastParallelQueueExecutor);
    }

    @Override
    public void attachListener() {
        // nothing to do
    }
}
