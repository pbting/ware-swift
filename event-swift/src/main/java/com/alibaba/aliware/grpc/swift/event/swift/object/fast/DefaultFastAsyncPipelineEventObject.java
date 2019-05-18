package com.alibaba.aliware.grpc.swift.event.swift.object.fast;

import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.object.pipeline.IPipelineEventListener;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;

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
