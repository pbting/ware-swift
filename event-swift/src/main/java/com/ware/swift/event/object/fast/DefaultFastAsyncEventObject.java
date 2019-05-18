package com.ware.swift.event.object.fast;

import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.IEventPartitionerRegister;
import com.ware.swift.event.object.IEventObjectListener;
import com.ware.swift.event.parallel.IParallelQueueExecutor;

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
