package com.ware.swift.grpc;

import com.ware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 *
 */
public class GrpcRequestResponseInteractive extends AbstractGrpcInteractive {

    public GrpcRequestResponseInteractive(InteractivePayload raftInteractivePayload,
                                          CallStreamObserver responseStream) {
        super(raftInteractivePayload, responseStream);
    }

    @Override
    public boolean sendPayload(InteractivePayload interactivePayload) {
        if (responseStream.isReady()) {
            responseStream.onNext(interactivePayload);
            responseStream.onCompleted();
            return true;
        }

        return false;
    }
}
