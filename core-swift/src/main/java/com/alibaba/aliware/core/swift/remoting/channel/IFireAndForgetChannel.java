package com.alibaba.aliware.core.swift.remoting.channel;


import com.alibaba.aliware.swift.proto.InteractivePayload;

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
