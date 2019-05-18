package com.alibaba.aliware.grpc.swift.event.swift.object.fast;

import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitioner;
import com.alibaba.aliware.grpc.swift.event.swift.IEventPartitionerRegister;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;

class DefaultAsyncEventObjectImpl<E>
		implements IAsyncEventObject<E>, IEventPartitionerRegister {

	private IParallelQueueExecutor parallelQueueExecutor;
	private IEventPartitioner ieventPartitioner;
	private IEventObject<E> eventObject;

	public DefaultAsyncEventObjectImpl(IParallelQueueExecutor parallelQueueExecutor,
			IEventObject<E> eventObject) {
		super();
		this.parallelQueueExecutor = parallelQueueExecutor;
		this.eventObject = eventObject;
	}

	@Override
	public void shutdown() {

		parallelQueueExecutor.stop();
	}

	@Override
	public void adjustExecutor(int coreSize, int maxSize) {

		throw new UnsupportedOperationException("does not adjuest the executor.");
	}

	@Override
	public void enEmergencyQueue(Runnable runnable) {

		parallelQueueExecutor.enEmergenceyQueue(runnable);
	}

	@Override
	public IParallelQueueExecutor getParallelQueueExecutor() {
		final IParallelQueueExecutor tmpParallelQueueExecutor = parallelQueueExecutor;
		return tmpParallelQueueExecutor;
	}

	@Override
	public void publish(E value, Integer eventType, IEventCallBack iEventCallBack) {

		notifyListeners(new ObjectEvent<>(this, value, eventType), iEventCallBack);
	}

	@Override
	public void notifyListeners(ObjectEvent<E> objectEvent,
			IEventCallBack iEventCallBack) {
		objectEvent.setParameter(ObjectEvent.EVENT_CALLBACK, iEventCallBack);
		eventObject.notifyListeners(objectEvent);
	}

	@Override
	public String partitioner(ObjectEvent<E> event) {
		if (ieventPartitioner != null) {

			return ieventPartitioner.partitioner(event);
		}

		return event.getEventTopic();
	}

	public IEventObject<E> getEventObject() {
		return eventObject;
	}

	public void setEventObject(IEventObject<E> eventObject) {
		this.eventObject = eventObject;
	}

	@Override
	public void registerEventPartitioner(IEventPartitioner eventPartitioner) {

		this.ieventPartitioner = eventPartitioner;
	}

}
