package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;

import java.util.EventListener;

/**
 * 
 * @param <V>
 */
public interface IEventObject<V> {

	int DEFAULT_EVENT_TYPE = Integer.MAX_VALUE - 1;

	/**
	 * 添加一个默认的 event listener, 当一个事件没有找到具体的 event listener,并且又添加了默认的，这个时候会触发默认的 event
	 * listener.
	 */
	default void setDefaultListener(EventListener eventListener) {
		// nothing to do
	}

	/**
	 * 在事件对象上面添加事件监听器
	 */
	void attachListener();

	/**
	 * 移除一组 object listeners
	 * @param eventType
	 */
	void removeListener(Integer eventType);

	/**
	 * 
	 * @param event
	 */
	default void dispatcher(ObjectEvent<V> event) {
		notifyListeners(event);
	}

	/**
	 * 唤醒一组事件监听器。这组事件监听器按序执行
	 * @param event
	 */
	void notifyListeners(ObjectEvent<V> event);

	/**
	 * 清除事件监听器
	 */
	void clearListener();

	/**
	 * 给某个事件类型发布一个消息。这个消息会触发一组事件监听器执行
	 * @param v
	 * @param eventType
	 */
	void publish(V v, Integer eventType);

	/**
	 * 
	 * @param eventType
	 * @return
	 */
	boolean containsEventType(Integer eventType);
}
