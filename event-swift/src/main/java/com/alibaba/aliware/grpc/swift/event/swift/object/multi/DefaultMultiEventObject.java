package com.alibaba.aliware.grpc.swift.event.swift.object.multi;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;

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
