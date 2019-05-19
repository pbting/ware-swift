package com.ware.swift.event.object;

public class DefaultEventObject<V> extends AbstractEventObject<V> {

	public DefaultEventObject(boolean isOptimism) {
		super(isOptimism);
	}

	@Override
	public void attachListener() {
		// nothing to do
	}
}
