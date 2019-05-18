package com.alibaba.aliware.grpc.swift.event.swift.object.multi;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObjectListener;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.IEventObjectListener;

import java.util.Deque;

public interface IMultiCastEventObject<V> {

	void multiCast(Deque<IEventObjectListener<V>> eventObjectListeners,
			ObjectEvent<V> event);
}
