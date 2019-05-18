package com.alibaba.aliware.grpc.swift.event.swift;

/**
 * 
 * @author pbting
 *
 */
public interface IEventPartitioner {

	<V> String partitioner(ObjectEvent<V> objectEvent);
}
