package com.alibaba.aliware.grpc.swift.event.swift.object.fast;

import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.AbstractEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObjectListener;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObjectListener;

import java.util.Deque;

public abstract class AbstractFastAsyncEventObject<V> extends AbstractEventObject<V>
		implements IAsyncEventObject<V> {

	protected DefaultAsyncEventObjectImpl<V> defaultAsyncEventObject;

	public AbstractFastAsyncEventObject(
			IParallelQueueExecutor superFastParallelQueueExecutor) {
		this(superFastParallelQueueExecutor, true);
	}

	public AbstractFastAsyncEventObject(
			IParallelQueueExecutor superFastParallelQueueExecutor, boolean isOptimism) {
		super(isOptimism);
		this.defaultAsyncEventObject = new DefaultAsyncEventObjectImpl<V>(
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

	/**
	 * 改造为异步
	 */
	@Override
	public void listenerHandler(final Deque<IEventObjectListener<V>> eventObjectListeners,
			final ObjectEvent<V> event) {
		getParallelQueueExecutor().execute(partitioner(event), new Runnable() {
			@Override
			public void run() {

				doListenerHandler(eventObjectListeners, event);
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
