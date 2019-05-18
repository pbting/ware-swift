package com.alibaba.aliware.grpc.swift.event.swift.object.pipeline;

import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IParallelActionExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

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

	public void subscriber(IPipelineEventListener<V> pipelineObjectListener, int eventType){
		
		this.addLast(pipelineObjectListener, eventType);
	}
	
	@Override
	public String partitioner(ObjectEvent<V> event) {
		if(this.iEventPartitioner != null){
			
			return this.iEventPartitioner.partitioner(event);
		}
		
		return super.partitioner(event);
	}
	
	@Override
	public void registerEventPartitioner(IEventPartitioner eventPartitioner) {
		
		this.iEventPartitioner = eventPartitioner;
	}
	
}
