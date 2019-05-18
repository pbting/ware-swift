package com.alibaba.aliware.core.swift.remoting.channel;

import com.alibaba.aliware.core.swift.remoting.IRequestStreamCallbackRegistrator;
import com.alibaba.aliware.swift.proto.InteractivePayload;

/**
 * 具有服务端 push stream 通信能力的 channel
 */
public interface IRequestStreamChannel extends IChannelModule {

    /**
     * @param inBound
     * @return
     */
    IRequestStreamCallbackRegistrator requestStream(InteractivePayload inBound);
}
