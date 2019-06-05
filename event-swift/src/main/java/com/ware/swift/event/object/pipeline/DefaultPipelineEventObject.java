package com.ware.swift.event.object.pipeline;

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

}
