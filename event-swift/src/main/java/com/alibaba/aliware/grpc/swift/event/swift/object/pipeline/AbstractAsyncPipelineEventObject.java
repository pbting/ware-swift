package com.alibaba.aliware.grpc.swift.event.swift.object.pipeline;

import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.FutureObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.ActionExecuteException;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.FastParallelActionExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IParallelActionExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.IAsyncEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.FutureObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;

import java.util.Deque;

public abstract class AbstractAsyncPipelineEventObject<V>
		extends AbstractPipelineEventObject<V> implements IAsyncEventObject<V> {
	protected IParallelActionExecutor executor;

	public AbstractAsyncPipelineEventObject(IParallelActionExecutor executor) {
		super();
		this.executor = executor;
	}

	public AbstractAsyncPipelineEventObject(boolean isOptimism,
			IParallelActionExecutor executor) {
		super(isOptimism);
		this.executor = executor;
	}

	public AbstractAsyncPipelineEventObject(boolean isOptimism, String executorName) {
		super(isOptimism);
		int coreSize = Runtime.getRuntime().availableProcessors() * 4;
		this.executor = new FastParallelActionExecutor(coreSize, executorName);
	}

	/**
	 * 提供异步模式的事件 发布
	 * @param value
	 * @param eventTopic
	 * @return
	 */
	public FutureObjectEvent<V> publishWithFuture(V value, Integer eventTopic) {
		FutureObjectEvent<V> futureObjectEvent = new FutureObjectEvent<>(this, value,
				eventTopic);
		notifyListeners(futureObjectEvent);
		return futureObjectEvent;
	}

	@Override
	public void listenerHandler(final Deque<IPipelineEventListener<V>> objectListeners,
			final ObjectEvent<V> event) {
		executor.enParallelAction(partitioner(event), new Action() {
			@Override
			public void execute() throws ActionExecuteException {
				doListenerHandler(objectListeners, event);
			}
		});
	}

	@Override
	public String partitioner(ObjectEvent<V> event) {

		return event.getEventTopic();
	}

	@Override
	public void adjustExecutor(int coreSize, int maxSize) {
		executor.adjustPoolSize(coreSize, maxSize);
	}

	@Override
	public void enEmergencyQueue(Runnable runnable) {
		executor.enEmergenceyQueue(runnable);
	}

	public void shutdown() {
		this.executor.stop();
	}

	@Override
	public IParallelQueueExecutor getParallelQueueExecutor() {

		return this.executor;
	}

	@Override
	public void publish(V value, Integer eventType, IEventCallBack iEventCallBack) {

		notifyListeners(new ObjectEvent<>(this, value, eventType), iEventCallBack);
	}

	@Override
	public void notifyListeners(ObjectEvent<V> objectEvent,
			IEventCallBack iEventCallBack) {
		objectEvent.setParameter(ObjectEvent.EVENT_CALLBACK, iEventCallBack);
		notifyListeners(objectEvent);
	}
}
