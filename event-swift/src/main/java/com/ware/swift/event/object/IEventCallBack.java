package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;

public interface IEventCallBack {

	<V> void eventCallBack(ObjectEvent<V> objectEvent);
}
