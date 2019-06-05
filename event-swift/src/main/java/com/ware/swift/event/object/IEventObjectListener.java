package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;

import java.util.EventListener;

/**
 * 定义事件处理接口。由用户真正的实现
 *
 * @param <V>
 * @author pengbingting
 */
public interface IEventObjectListener<V> extends EventListener {

    void onEvent(ObjectEvent<V> event) throws Throwable;
}
