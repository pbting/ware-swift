package com.ware.swift.core.remoting.channel;


import com.ware.swift.proto.InteractivePayload;

/**
 * 具有服务端可以不 response 通信能力的 channel
 */
public interface IFireAndForgetChannel extends IChannelModule {

    /**
     * @param inBound
     * @return
     */
    void fireAndForget(InteractivePayload inBound);
}
