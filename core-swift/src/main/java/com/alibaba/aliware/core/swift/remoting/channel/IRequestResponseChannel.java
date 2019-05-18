package com.alibaba.aliware.core.swift.remoting.channel;


import com.alibaba.aliware.swift.proto.InteractivePayload;

/**
 * 具有 request/response 通信能力的 channel
 */
public interface IRequestResponseChannel extends IChannelModule {

    /**
     * @param inBound
     * @param <OUT_BOUND>
     * @return
     */
    <OUT_BOUND> OUT_BOUND requestResponse(InteractivePayload inBound);
}
