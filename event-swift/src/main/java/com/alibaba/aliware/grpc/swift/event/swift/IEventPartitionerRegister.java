package com.alibaba.aliware.grpc.swift.event.swift;

/**
 *
 */
public interface IEventPartitionerRegister {

    /**
     * @param eventPartitioner
     */
    void registerEventPartitioner(IEventPartitioner eventPartitioner);
}
