package com.ware.swift.grpc;

import com.ware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 * 基于 Grpc 实现的两个节点间的通信模型。默认是异步的
 */
public class GrpcRequestStreamInteractive extends AbstractGrpcInteractive {

    public GrpcRequestStreamInteractive(InteractivePayload interactivePayload,
                                        CallStreamObserver responseStream) {
        super(interactivePayload, responseStream);
    }

    /**
     * @param interactivePayload
     * @return
     */
    public boolean sendPayload(InteractivePayload interactivePayload) {
        if (responseStream.isReady()) {
            responseStream.onNext(interactivePayload);
            return true;
        }

        return false;
    }
}
