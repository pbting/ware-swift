package com.ware.swift.event.object.pipeline;

import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.object.IEventObject;

import java.util.Deque;

/**
 * 
 * @param <V>
 */
public interface IPipelineEventObject<V> extends IEventObject<V> {

	/**
	 * 添加某个事件类型的监听器。一个 eventType 可对应多个 object listener
	 * @param objectListener
	 * @param eventType
	 */
	void addListener(IPipelineEventListener<V> objectListener, Integer eventType);

	/**
	 * 移除指定 event type 中的一个object listener
	 * @param objectListener
	 * @param eventType
	 */
	void removeListener(IPipelineEventListener<V> objectListener, Integer eventType);

	/**
	 * 在某个事件类型后面添加一个新的 对象监听器
	 * @param objectListener
	 * @param eventType
	 */
	void addLast(IPipelineEventListener<V> objectListener, Integer eventType);

	/**
	 * 在某个事件类型前面添加一个新的事件监听器
	 */
	void addFirst(IPipelineEventListener<V> objectListener, Integer eventType);

	/**
	 * listener handler the event
	 */
	void listenerHandler(Deque<IPipelineEventListener<V>> objectListeners,
			ObjectEvent<V> event);

}
