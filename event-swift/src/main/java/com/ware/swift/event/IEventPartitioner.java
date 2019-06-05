package com.ware.swift.event;

/**
 * 
 * @author pbting
 *
 */
public interface IEventPartitioner {

	<V> String partitioner(ObjectEvent<V> objectEvent);
}
