package com.alibaba.aliware.grpc.swift.event.swift.object;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

import java.util.Deque;

public interface IEventListenerObject<V> extends IEventObject<V> {

	/**
	 * 添加某个事件类型的监听器。一个 eventType 可对应多个 object listener
	 * @param objectListener
	 * @param eventType
	 */
	void addListener(IEventObjectListener<V> objectListener, Integer eventType);

	/**
	 * 移除指定 event type 中的一个object listener
	 * @param objectListener
	 * @param eventType
	 */
	void removeListener(IEventObjectListener<V> objectListener, Integer eventType);

	/**
	 * listener handler the event
	 */
	void listenerHandler(Deque<IEventObjectListener<V>> objectListeners,
			ObjectEvent<V> event);

}
