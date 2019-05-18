package com.alibaba.aliware.grpc.swift.event.swift.object;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

public interface IEventCallBack {

	<V> void eventCallBack(ObjectEvent<V> objectEvent);
}
