package com.alibaba.aliware.grpc.swift.event.swift.object;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

/**
 * 提供降级的接口实现
 * @author pengbingting
 *
 */
public interface IFallBackEventObjectListener<V> extends IEventObjectListener<V>{

	public void fallBack(ObjectEvent<V> event);
}
