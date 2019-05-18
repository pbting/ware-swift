package com.ware.swift.event.object.fast;

import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.IEventPartitionerRegister;
import com.ware.swift.event.object.pipeline.IPipelineEventListener;
import com.ware.swift.event.parallel.IParallelQueueExecutor;

/**
 * 
 * @param <V>
 */
public class DefaultFastAsyncPipelineEventObject<V> extends
		AbstractFastAsyncPipelineEventObject<V> implements IEventPartitionerRegister {

	public DefaultFastAsyncPipelineEventObject(
			IParallelQueueExecutor superFastParallelQueueExecutor, boolean isOptimism) {
		super(superFastParallelQueueExecutor, isOptimism);
	}

	public DefaultFastAsyncPipelineEventObject(
			IParallelQueueExecutor superFastParallelQueueExecutor) {
		super(superFastParallelQueueExecutor);
	}

	@Override
	public void attachListener() {
		// nothing to do
	}

	public void subscriber(IPipelineEventListener<V> pipelineObjectListener,
			int eventType) {

		this.addLast(pipelineObjectListener, eventType);
	}

	@Override
	public void registerEventPartitioner(IEventPartitioner eventPartitioner) {

		defaultAsyncEventObject.registerEventPartitioner(eventPartitioner);
	}
}
