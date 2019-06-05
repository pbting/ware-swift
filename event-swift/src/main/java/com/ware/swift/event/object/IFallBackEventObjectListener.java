package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;

/**
 * 提供降级的接口实现
 *
 * @author pengbingting
 */
public interface IFallBackEventObjectListener<V> extends IEventObjectListener<V> {

    void fallBack(ObjectEvent<V> event);
}
