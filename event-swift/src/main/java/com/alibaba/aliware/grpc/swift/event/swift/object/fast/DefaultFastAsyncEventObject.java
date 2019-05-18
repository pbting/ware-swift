package com.alibaba.aliware.grpc.swift.event.swift.object.fast;

import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObjectListener;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObjectListener;

public class DefaultFastAsyncEventObject<V> extends AbstractFastAsyncEventObject<V> implements IEventPartitionerRegister {
	
	public DefaultFastAsyncEventObject(IParallelQueueExecutor superFastParallelQueueExecutor, boolean isOptimism) {
		super(superFastParallelQueueExecutor, isOptimism);
	}

	public DefaultFastAsyncEventObject(IParallelQueueExecutor superFastParallelQueueExecutor) {
		super(superFastParallelQueueExecutor);
	}

	@Override
	public void attachListener() {
		//nothing to do
	}
	
	public void subscriber(IEventObjectListener<V> eventObjectListener, int eventType){
		
		this.addListener(eventObjectListener, eventType);
	}

	@Override
	public void registerEventPartitioner(IEventPartitioner eventPartitioner) {
		
		defaultAsyncEventObject.registerEventPartitioner(eventPartitioner);
	}
}
