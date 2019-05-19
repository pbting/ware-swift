package com.ware.swift.core.remoting;

import com.ware.swift.proto.InteractivePayload;

/**
 * 引用 Grpc 中设计的 {@link io.grpc.stub.StreamObserver}
 */
public interface IRemotingCallStreamObserver {

    default boolean isReady() {
        return true;
    }

    default void setOnReadyHandler(Runnable onReadyHandler) {
        // nothing to do
    }

    default void disableAutoInboundFlowControl() {
        // nothing to do
    }

    default void request(int count) {
        // nothing to do
    }

    default void setMessageCompression(boolean enable) {
        // nothing to do
    }

    /**
     * must be set
     *
     * @param interactivePayload
     */
    void onNext(InteractivePayload interactivePayload);

    default void onError(Throwable t) {
        // nothing to do
    }

    default void onCompleted() {
        // nothing to do
    }
}
