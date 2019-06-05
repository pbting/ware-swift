package com.ware.swift.core.remoting.channel;

import com.ware.swift.core.remoting.IRequestStreamCallbackRegistrator;
import com.ware.swift.proto.InteractivePayload;

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
