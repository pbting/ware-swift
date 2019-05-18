package com.alibaba.aliware.grpc.swift.event.swift.object;

import java.util.EventListener;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

/**
 * 定义事件处理接口。由用户真正的实现
 * @author pengbingting
 *
 * @param <V>
 */
public interface IEventObjectListener<V> extends EventListener {
	
	public void onEvent(ObjectEvent<V> event) throws Throwable;

}
