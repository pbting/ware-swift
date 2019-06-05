package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.common.Log;

import java.util.Collection;
import java.util.Deque;
import java.util.EventListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * 绝对业务流水线编程模型：即不管前一个是否执行成功，后一个业务逻辑都会执行。直至这个工序的所有动作都走完。
 * @author pengbingting
 *
 * @param <V>
 */
public abstract class AbstractEventObject<V> implements IEventListenerObject<V> {

	private static final Logger log = Logger
			.getLogger(AbstractEventObject.class.getCanonicalName());

	private ReentrantLock lock = new ReentrantLock(true);

	protected ConcurrentHashMap<Integer, Collection<IEventObjectListener<V>>> eventObjectListeners;

	public AbstractEventObject() {
		this(false);
	}

	/**
	 * 支持乐观触发和悲观触发两种模式.
	 * @param isOptimism true 表示乐观触发，false 表示悲观触发
	 */
	public AbstractEventObject(boolean isOptimism) {
		this.attachListener();
	}

	public abstract void attachListener();

	@Override
	public boolean containsEventType(Integer eventType) {
		if (eventType == null) {
			return false;
		}

		return eventObjectListeners == null ? false
				: eventObjectListeners.containsKey(eventType);
	}

	/**
	 * 如果子类不支持对象池技术，那么重写该方法可能会带来更佳的性能
	 */
	public void publish(V v, Integer eventType) {
		ObjectEvent<V> objectEvent = new ObjectEvent<>(this, v, eventType);
		notifyListeners(objectEvent);
	}

	@Override
	public void setDefaultListener(EventListener eventListener) {
		assert eventListener instanceof IEventObjectListener;
		addListener((IEventObjectListener) eventListener,
                DEFAULT_EVENT_TYPE);

	}

	public void addListener(IEventObjectListener<V> objectListener, Integer eventType) {
		lock.lock();
		try {
			if (eventObjectListeners == null) {
				eventObjectListeners = new ConcurrentHashMap<>();
			}
		}
		finally {
			lock.unlock();
		}
		if (eventObjectListeners.get(eventType) == null) {
			Collection<IEventObjectListener<V>> tempInfo = new ConcurrentLinkedDeque();
			tempInfo.add(objectListener);
			eventObjectListeners.put(eventType, tempInfo);
		}
		else {
			eventObjectListeners.get(eventType).add(objectListener);
		}
		debugEventMsg("注册一个事件,类型为" + eventType);
	}

	public void removeListener(IEventObjectListener<V> objectListener,
			Integer eventType) {
		if (eventObjectListeners == null)
			return;

		Collection<IEventObjectListener<V>> tempInfo = eventObjectListeners
				.get(eventType);
		lock.lock();
		try {
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
		Collection<IEventObjectListener<V>> listener = eventObjectListeners
				.remove(eventType);
		if (listener != null && listener.size() > 0) {
			listener.clear();
		}

		debugEventMsg("移除一个事件,类型为" + eventType);
	}

	public void notifyListeners(ObjectEvent<V> event) {
		// 这里异步情况下，为了尽量不足赛上层代码，可以改成异步
		doNotify(event);
	}

	@SuppressWarnings("unchecked")
	protected final void doNotify(ObjectEvent<V> event) {
		if (eventObjectListeners == null) {
			return;
		}

		int eventType = event.getEventType();
		Deque tempDeque = (Deque<IEventObjectListener<V>>) eventObjectListeners
				.getOrDefault(eventType, eventObjectListeners.get(DEFAULT_EVENT_TYPE));

		if (tempDeque == null || tempDeque.isEmpty()) {
			log.warning(event.getEventType()
					+ " does not relevant event listener matching 。");
			return;
		}
		// 3.1、触发,可以改造为异步触发.只需覆盖这个方法即可
		listenerHandler(tempDeque, event);
	}

	public void listenerHandler(Deque<IEventObjectListener<V>> eventObjectListeners,
			ObjectEvent<V> event) {

		doListenerHandler(eventObjectListeners, event);
	}

	protected final void doListenerHandler(
			Deque<IEventObjectListener<V>> eventObjectListeners, ObjectEvent<V> event) {

		// 1、
		for (IEventObjectListener<V> listener : eventObjectListeners) {
			try {
				listener.onEvent(event);
			}
			catch (Throwable e) {
				if (listener instanceof IFallBackEventObjectListener) {
					IFallBackEventObjectListener<V> fallBackHandler = (IFallBackEventObjectListener<V>) listener;
					fallBackHandler.fallBack(event);
				}
				else {
					Log.error("event topic=" + event.getEventType()
							+ "cause an exception[" + event.getValue().toString() + "]",
							e);
					Log.debug("burst fall back handler for this event topic="
							+ event.getEventType() + " and the value="
							+ event.getValue().toString());
				}

			}
		}
		// 2、
		Object value = event.getParameter(ObjectEvent.EVENT_CALLBACK);
		if (value != null && value instanceof IEventCallBack) {
			IEventCallBack eventCallBack = (IEventCallBack) value;
			eventCallBack.eventCallBack(event);
		}
	}

	public void clearListener() {
		lock.lock();
		try {
			if (eventObjectListeners != null) {
				eventObjectListeners = null;
			}
		}
		finally {
			lock.unlock();
		}
	}

	protected void debugEventMsg(String msg) {
		Log.debug(msg);
	}
}
