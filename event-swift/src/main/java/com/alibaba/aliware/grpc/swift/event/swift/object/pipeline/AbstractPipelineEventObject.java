package com.alibaba.aliware.grpc.swift.event.swift.object.pipeline;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObject;

import java.util.Collection;
import java.util.Deque;
import java.util.EventListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于业务流水线的编程模式： 如果你的一些业务前后有所依赖(A->B->C)，即 C 的 执行必须依赖于 B 的正确执行，而 B 的执行有依赖与 A 的正确执行。那么继承这个
 * 类是非常有意义的。实现类似于业务流水线的编程模式。将各道工序业务脱离出来。实现业务内高内聚低耦合的设计模式。 高内聚的实现：即将各个相关的业务统一由一个 事件类型 给 穿线
 * 穿起来。从而将他们一次串起来，达到高内聚的效果 低耦合的实现：前后业务通过各个 pipeline object listener 进行隔离。前后耦合仅仅是发一个 true
 * or false 来表示后一个业务(逻辑)是否需要执行 【注意】 基于业务流水线的 不提供降级处理策略
 *
 * @param <V>
 * @author pengbingting
 */
public abstract class AbstractPipelineEventObject<V>
		implements IPipelineEventObject<V> {

	protected ConcurrentHashMap<Integer, Collection<IPipelineEventListener<V>>> listeners;
	protected static boolean isDebug = false;
	protected ReentrantLock lock = new ReentrantLock();

	public AbstractPipelineEventObject() {
		this(false);
	}

	/**
	 * 支持乐观触发和悲观触发两种模式.
	 *
	 * @param isOptimism true 表示乐观触发，false 表示悲观触发
	 */
	public AbstractPipelineEventObject(boolean isOptimism) {
		this.attachListener();
	}

	public abstract void attachListener();

	@Override
	public boolean containsEventType(Integer eventType) {
		if (eventType == null || listeners == null) {
			return false;
		}

		return listeners.containsKey(eventType);
	}

	@Override
	public void setDefaultListener(EventListener eventListener) {
		assert eventListener instanceof IPipelineEventListener;
		addLast((IPipelineEventListener) eventListener, IEventObject.DEFAULT_EVENT_TYPE);
	}

	public void addLast(IPipelineEventListener<V> objectListener, Integer eventType) {
		addListener0(objectListener, eventType, 1);
	}

	@Override
	public void addFirst(IPipelineEventListener<V> objectListener, Integer eventType) {
		addListener0(objectListener, eventType, 0);
	}

	/**
	 * this method has deprecated,please use the notifyListeners
	 */
	public void publish(V v, Integer eventType) {
		ObjectEvent<V> objectEvent = new ObjectEvent<>(this, v, eventType);
		notifyListeners(objectEvent);
	}

	public void addListener(IPipelineEventListener<V> objectListener, Integer eventType) {
		addListener0(objectListener, eventType, 1);
	}

	private void addListener0(IPipelineEventListener<V> objectListener, Integer eventType,
			int order) {
		if (listeners == null) {
			lock.lock();
			try {
				if (listeners == null) {
					listeners = new ConcurrentHashMap<>();
				}
			}
			finally {
				lock.unlock();
			}
		}

		if (listeners.get(eventType) == null) {
			ConcurrentLinkedDeque<IPipelineEventListener<V>> tempInfo = new ConcurrentLinkedDeque<>();
			if (order > 0) {
				tempInfo.addLast(objectListener);
			}
			else {
				tempInfo.addFirst(objectListener);
			}
			listeners.put(eventType, tempInfo);
		}
		else {
			listeners.get(eventType).add(objectListener);
		}

		debugEventMsg("注册一个事件,类型为" + eventType);
	}

	public void removeListener(IPipelineEventListener<V> objectListener,
			Integer eventType) {
		if (listeners == null)
			return;
		lock.lock();
		try {
			Collection<IPipelineEventListener<V>> tempInfo = listeners.get(eventType);
			if (tempInfo == null) {
				return;
			}
			if (tempInfo.size() == 1) {
				tempInfo.clear();
				return;
			}
			tempInfo.remove(objectListener);
		}
		finally {
			lock.unlock();
		}

		debugEventMsg("移除一个事件,类型为" + eventType);
	}

	public void removeListener(Integer eventType) {
		Collection<IPipelineEventListener<V>> listener = listeners.remove(eventType);
		if (listener != null) {
			listener.clear();
		}
		debugEventMsg("移除一个事件,类型为" + eventType);
	}

	@SuppressWarnings("unchecked")
	private final void doNotify(ObjectEvent<V> event) {
		if (listeners == null) {
			return;
		}

		int eventType = event.getEventType();
		Deque tempList = (Deque<IPipelineEventListener<V>>) listeners
				.getOrDefault(eventType, listeners.get(DEFAULT_EVENT_TYPE));

		if (tempList == null || tempList.isEmpty()) {
			return;
		}

		// 3、触发,
		listenerHandler(tempList, event);
	}

	public void notifyListeners(ObjectEvent<V> event) {
		doNotify(event);
	}

	/**
	 * 处理 单个的事件
	 */
	public void listenerHandler(Deque<IPipelineEventListener<V>> objectListeners,
			ObjectEvent<V> event) {

		doListenerHandler(objectListeners, event);
	}

	protected final void doListenerHandler(
			Deque<IPipelineEventListener<V>> objectListeners, ObjectEvent<V> event) {
		int index = 1;
		for (IPipelineEventListener<V> listener : objectListeners) {
			try {
				boolean isSuccessor = listener.onEvent(event, index);
				if (!isSuccessor || event.isInterruptor()) {
					break;
				}
			}
			catch (Exception e) {
				Log.error("handler the event cause an exception."
						+ event.getValue().toString(), e);
			}
			index++;
		}

		// 1、触发事件回调
		Object value = event.getParameter(ObjectEvent.EVENT_CALLBACK);
		if (value != null && value instanceof IEventCallBack) {
			IEventCallBack eventCallBack = (IEventCallBack) value;
			try {
				eventCallBack.eventCallBack(event);
			}
			catch (Exception e) {
				Log.error("handler the event cause an exception."
						+ event.getValue().toString(), e);
			}
		}
	}

	public void clearListener() {
		lock.lock();
		try {
			if (listeners != null) {
				listeners = null;
			}
		}
		finally {
			lock.unlock();
		}
	}

	protected void debugEventMsg(String msg) {
		if (isDebug) {
			System.out.println(msg);
		}
	}

}
