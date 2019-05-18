package com.alibaba.aliware.grpc.swift.event.swift.object;

public class DefaultEventObject<V> extends AbstractEventObject<V> {

	public DefaultEventObject(boolean isOptimism) {
		super(isOptimism);
	}

	@Override
	public void attachListener() {
		// nothing to do
	}
}
