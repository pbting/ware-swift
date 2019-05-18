package com.ware.swift.core.remoting.channel;

import com.ware.swift.core.remoting.IRequestStreamCallbackRegistrator;
import com.ware.swift.proto.InteractivePayload;

/**
 *
 */
public abstract class AbstractRemotingChannelModel extends AbstractRemotingChannel {

    public AbstractRemotingChannelModel(String addressPort) {
        super(addressPort);
    }

    @Override
    public InteractivePayload requestResponse(InteractivePayload inBound) {
        throw new UnsupportedOperationException("request/response does not implement");
    }

    @Override
    public <OUT_BOUND, IN_BOUND> OUT_BOUND requestChannel(IN_BOUND inBound) {
        throw new UnsupportedOperationException("request/channel does not implement");
    }

    @Override
    public IRequestStreamCallbackRegistrator requestStream(InteractivePayload inBound) {
        throw new UnsupportedOperationException("request/stream does not implement");
    }

    @Override
    public void fireAndForget(InteractivePayload inBound) {
        throw new UnsupportedOperationException("fireAndForget does not implement");
    }
}
