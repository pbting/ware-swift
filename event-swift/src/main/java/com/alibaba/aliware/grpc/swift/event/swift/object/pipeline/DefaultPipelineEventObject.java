package com.alibaba.aliware.grpc.swift.event.swift.object.pipeline;

public class DefaultPipelineEventObject<V> extends AbstractPipelineEventObject<V> {

	public DefaultPipelineEventObject() {
		super();
	}

	public DefaultPipelineEventObject(boolean isOptimism) {
		super(isOptimism);
	}

	@Override
	public void attachListener() {
		// nothing to do
	}

	public AbstractPipelineEventObject subscriber(
            IPipelineEventListener<V> pipelineObjectListener, int eventType) {

		this.addLast(pipelineObjectListener, eventType);
		return this;
	}
}
