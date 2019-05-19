package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;

import java.util.EventListener;

/**
 * 定义事件处理接口。由用户真正的实现
 * @author pengbingting
 *
 * @param <V>
 */
public interface IEventObjectListener<V> extends EventListener {
	
	public void onEvent(ObjectEvent<V> event) throws Throwable;

}
