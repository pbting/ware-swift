package com.ware.swift.event.object.pipeline;

import com.ware.swift.event.ObjectEvent;

import java.util.EventListener;

/**
 * 前驱事件监听器。即 只有当前一个事件监听器处理完后，返回 true，后面的才可以继续执行
 * @author pengbingting
 *
 * @param <V>
 */
public interface IPipelineEventListener<V> extends EventListener {

	/**
	 * @param event an object trigger an event
	 * @param listenerIndex current object listener index
	 * @return 如果返回 true,则会触发后一个事件监听器执行，否则终止执行
	 */
	boolean onEvent(ObjectEvent<V> event, int listenerIndex);
}
