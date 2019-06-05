package com.ware.swift.grpc;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.proto.InteractivePayload;
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
