package com.alibaba.aliware.grpc.swift;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 *
 */
public abstract class AbstractGrpcInteractive implements IInteractive {

    protected InteractivePayload interactivePayload;

    protected CallStreamObserver responseStream;

    public AbstractGrpcInteractive() {
    }

    public AbstractGrpcInteractive(InteractivePayload interactivePayload,
                                   CallStreamObserver responseStream) {
        this.interactivePayload = interactivePayload;
        this.responseStream = responseStream;
    }

    public InteractivePayload getInteractivePayload() {
        return interactivePayload;
    }

    public void setInteractivePayload(InteractivePayload interactivePayload) {
        this.interactivePayload = interactivePayload;
    }

    /**
     * @param interactivePayload
     * @return
     */
    public abstract boolean sendPayload(InteractivePayload interactivePayload);
}
