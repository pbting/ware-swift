package com.ware.swift.event.object.multi;

import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.object.IEventObjectListener;

import java.util.Deque;

public interface IMultiCastEventObject<V> {

	void multiCast(Deque<IEventObjectListener<V>> eventObjectListeners,
			ObjectEvent<V> event);
}
