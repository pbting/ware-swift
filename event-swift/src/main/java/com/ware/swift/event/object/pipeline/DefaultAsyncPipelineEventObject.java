package com.ware.swift.event.object.pipeline;

import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.IEventPartitionerRegister;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.parallel.action.IParallelActionExecutor;

public class DefaultAsyncPipelineEventObject<V> extends AbstractAsyncPipelineEventObject<V> implements IEventPartitionerRegister {

    protected IEventPartitioner iEventPartitioner;

    public DefaultAsyncPipelineEventObject(boolean isOptimism, IParallelActionExecutor executor) {
        super(isOptimism, executor);
    }

    public DefaultAsyncPipelineEventObject(boolean isOptimism, String executorName) {
        super(isOptimism, executorName);
    }

    public DefaultAsyncPipelineEventObject(IParallelActionExecutor executor) {
        super(executor);
    }

    @Override
    public void attachListener() {
        //nothing to do
    }

    @Override
    public String partitioner(ObjectEvent<V> event) {
        if (this.iEventPartitioner != null) {

            return this.iEventPartitioner.partitioner(event);
        }

        return super.partitioner(event);
    }

    @Override
    public void registerEventPartitioner(IEventPartitioner eventPartitioner) {

        this.iEventPartitioner = eventPartitioner;
    }

}
