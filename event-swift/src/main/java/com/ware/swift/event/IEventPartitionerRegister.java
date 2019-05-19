package com.ware.swift.event;

/**
 *
 */
public interface IEventPartitionerRegister {

    /**
     * @param eventPartitioner
     */
    void registerEventPartitioner(IEventPartitioner eventPartitioner);
}
