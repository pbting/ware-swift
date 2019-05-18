package com.alibaba.aliware.grpc.swift.event.swift.loop;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.lmax.disruptor.Sequence;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * the base implement of event loop queue.
 * @author pengbingting
 *
 * @param <E>
 */
public class EventLoopQueue<E> implements IEventLoopQueue<EventLoopHandler<E>> {

	private Queue<EventLoopHandler<E>> queue;
	private IParallelQueueExecutor executor;
	private ReentrantLock queueLock = new ReentrantLock();
	protected AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup = null;// ms
	protected Sequence lastActiveTime = new Sequence(System.currentTimeMillis());

	public EventLoopQueue(IParallelQueueExecutor executor,
			AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup) {
		this.executor = executor;
		queue = new LinkedList<>();
		this.asyncEventLoopGroup = asyncEventLoopGroup;
	}

	public EventLoopQueue(IParallelQueueExecutor executor,
			Queue<EventLoopHandler<E>> queue,
			AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup) {
		this.executor = executor;
		this.queue = queue;
		this.asyncEventLoopGroup = asyncEventLoopGroup;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public IEventLoopQueue getActionQueue() {
		return this;
	}

	@Override
	public void clear() {

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Queue getQueue() {
		return queue;
	}

	@Override
	public void enqueue(EventLoopHandler<E> eventHandler) {
		int queueSize;
		eventHandler.setEventLoopQueueIfAbsent(this);
		queueLock.lock();
		try {
			queue.add(eventHandler);
			queueSize = queue.size();
		}
		finally {
			queueLock.unlock();
		}
		if (queueSize == 1) {
			// ObjectEvent<E> objectEvent = eventHandler.getEvent();
			// executor.execute(eventHandler.getAsyncEventLoopGroup().partitioner(objectEvent),
			// eventHandler);
			doExecute(eventHandler);
		}
		if (queueSize > 4 * Runtime.getRuntime().availableProcessors()) {
			Log.warn(eventHandler.getEvent().toString() + " queue size : " + queueSize);
		}

		lastActiveTime.set(System.currentTimeMillis());
	}

	@Override
	public void dequeue(EventLoopHandler<E> eventHandler) {
		EventLoopHandler<E> nextEventHandler = null;
		int queueSize;
		String tmpString = null;
		queueLock.lock();
		try {
			queueSize = queue.size();
			EventLoopHandler<E> temp = queue.remove();
			if (temp != eventHandler) {
				tmpString = temp.getEvent().getValue().toString();
			}
			if (queueSize != 0) {
				nextEventHandler = queue.peek();
			}
		}
		finally {
			queueLock.unlock();
		}

		if (nextEventHandler != null) {
			ObjectEvent<E> objectEvent = nextEventHandler.getEvent();
			executor.execute(
					nextEventHandler.getAsyncEventLoopGroup().partitioner(objectEvent),
					nextEventHandler);
		}
		if (queueSize == 0) {
			Log.debug("queue.size() is 0.");
		}
		if (tmpString != null) {
			Log.debug("action queue error. temp " + tmpString + ", action : "
					+ eventHandler.getEvent().getValue().toString());
		}
	}

	@Override
	public void schedulerdEventLoopHandler(final EventLoopHandler<E> eventHandler) {
		executor.getScheduledExecutorService().schedule(() -> enqueue(eventHandler),
				getEventLoopInterval(eventHandler), TimeUnit.MILLISECONDS);
	}

	@Override
	public long getEventLoopInterval(EventLoopHandler<E> eventHandler) {
		ObjectEvent<E> objectEvent = eventHandler.getEvent();
		long interval = asyncEventLoopGroup.getSchedulerInterval();// 公用的
		// 可根据实际的情况，动态调整每次 loop interval 的时间 间隔
		Object value = objectEvent
				.getParameter(EventLoopConstants.EVENT_LOOP_INTERVAL_PARAM);
		if (value != null) {
			// 每个事件根据自己的需要可动态调整 event loop interval.即更加细粒度的控制 event interval 的时间 间隔
			if (value instanceof Integer) {
				int tmpValue = (int) value;
				interval = tmpValue;
			}
			else if (value instanceof Long) {
				interval = (long) value;
			}
			else if (value instanceof String) {
				try {
					interval = Long.valueOf(value.toString());
				}
				catch (NumberFormatException e) {
					interval = TimeUnit.SECONDS.toMillis(1);
				}
			}
		}

		return interval;
	}

	@Override
	public long getLastActiveTime() {

		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doExecute(Runnable runnable) {
		EventLoopHandler<E> eventHandler = (EventLoopHandler<E>) runnable;
		ObjectEvent<E> objectEvent = eventHandler.getEvent();
		executor.execute(eventHandler.getAsyncEventLoopGroup().partitioner(objectEvent),
				eventHandler);
	}

	@Override
	public ReentrantLock getActionQueueLock() {

		return queueLock;
	}
}
