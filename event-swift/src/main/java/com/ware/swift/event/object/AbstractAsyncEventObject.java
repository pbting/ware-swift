package com.ware.swift.event.object;

import com.ware.swift.event.IAsyncEventObject;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.parallel.action.Action;
import com.ware.swift.event.parallel.action.ActionExecuteException;
import com.ware.swift.event.parallel.action.FastParallelActionExecutor;
import com.ware.swift.event.parallel.action.IParallelActionExecutor;
import com.ware.swift.event.parallel.IParallelQueueExecutor;

import java.util.Deque;

/**
 * please note:if you extends this class then all of the object listener must handler the
 * problem in concurrent environment This asynchronous event handler is not recommended
 * for high concurrency(10,000,000), and asynchronous queues are recommended to achieve
 * the same effect. this class is deprecated,please use the AbstractDisruptorEventObject
 * class
 * @author pengbingting
 * @param <V>
 */
public abstract class AbstractAsyncEventObject<V> extends AbstractEventObject<V>
		implements IAsyncEventObject<V> {
	protected IParallelActionExecutor executor;

	public AbstractAsyncEventObject(String executorName, boolean isOptimism) {
		super(isOptimism);
		int coreSize = Runtime.getRuntime().availableProcessors() * 4;
		this.executor = new FastParallelActionExecutor(coreSize, executorName);
	}

	public AbstractAsyncEventObject(IParallelActionExecutor executor,
			boolean isOptimism) {
		super(isOptimism);
		this.executor = executor;
	}

	/**
	 * 提供异步模式的事件 发布
	 * @param value
	 * @param eventTopic
	 * @return
	 */
	public FutureObjectEvent<V> publishWithFuture(V value, int eventTopic) {
		FutureObjectEvent<V> futureObjectEvent = new FutureObjectEvent<>(this, value,
				eventTopic);
		notifyListeners(futureObjectEvent);
		return futureObjectEvent;
	}

	@Override
	public void listenerHandler(final Deque<IEventObjectListener<V>> eventObjectListener,
			final ObjectEvent<V> event) {

		executor.enParallelAction(partitioner(event), new Action() {
			@Override
			public void execute() throws ActionExecuteException {
				doListenerHandler(eventObjectListener, event);
			}
		});// ();
	}

	/**
	 * 根据事件 对并行队列进行分区
	 * @param event
	 * @return
	 */
	public String partitioner(ObjectEvent<V> event) {

		return event.getEventTopic();
	}

	@Override
	public void adjustExecutor(int coreSize, int maxSize) {
		this.executor.adjustPoolSize(coreSize, maxSize);
	}

	public void shutdown() {
		this.executor.stop();
	}

	@Override
	public void enEmergencyQueue(Runnable runnable) {
		executor.enEmergencyQueue(runnable);
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
