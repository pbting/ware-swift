package com.alibaba.aliware.grpc.swift.event.swift.object.fast;

import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.object.pipeline.AbstractPipelineEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.object.pipeline.IPipelineEventListener;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;

import java.util.Deque;

public abstract class AbstractFastAsyncPipelineEventObject<V>
		extends AbstractPipelineEventObject<V> implements IAsyncEventObject<V> {

	protected DefaultAsyncEventObjectImpl<V> defaultAsyncEventObject = null;

	public AbstractFastAsyncPipelineEventObject(
			IParallelQueueExecutor superFastParallelQueueExecutor) {
		this(superFastParallelQueueExecutor, true);
	}

	public AbstractFastAsyncPipelineEventObject(
			IParallelQueueExecutor superFastParallelQueueExecutor, boolean isOptimism) {
		super(isOptimism);
		this.defaultAsyncEventObject = new DefaultAsyncEventObjectImpl<>(
				superFastParallelQueueExecutor, this);
	}

	@Override
	public void shutdown() {

		defaultAsyncEventObject.shutdown();
	}

	@Override
	public void adjustExecutor(int coreSize, int maxSize) {

		defaultAsyncEventObject.adjustExecutor(coreSize, maxSize);
	}

	@Override
	public void enEmergencyQueue(Runnable runnable) {

		defaultAsyncEventObject.enEmergencyQueue(runnable);
	}

	@Override
	public void listenerHandler(final Deque<IPipelineEventListener<V>> objectListeners,
			final ObjectEvent<V> event) {
		getParallelQueueExecutor().execute(partitioner(event), new Runnable() {
			@Override
			public void run() {
				doListenerHandler(objectListeners, event);
			}
		});
	}

	@Override
	public String partitioner(ObjectEvent<V> event) {

		return defaultAsyncEventObject.partitioner(event);
	}

	@Override
	public IParallelQueueExecutor getParallelQueueExecutor() {

		return defaultAsyncEventObject.getParallelQueueExecutor();
	}

	@Override
	public void publish(V value, Integer eventType, IEventCallBack iEventCallBack) {

		defaultAsyncEventObject.publish(value, eventType, iEventCallBack);
	}

	@Override
	public void notifyListeners(ObjectEvent<V> objectEvent,
			IEventCallBack iEventCallBack) {

		defaultAsyncEventObject.notifyListeners(objectEvent, iEventCallBack);
	}

	@Override
	public abstract void attachListener();

}
