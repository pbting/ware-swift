package com.ware.swift.core.remoting.channel;


import com.ware.swift.proto.InteractivePayload;

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
