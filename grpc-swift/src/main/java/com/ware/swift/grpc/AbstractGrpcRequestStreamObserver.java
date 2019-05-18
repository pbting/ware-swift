package com.ware.swift.grpc;

import com.ware.swift.core.remoting.IRemotingCallStreamObserver;
import com.ware.swift.core.remoting.IRequestStreamCallbackRegistrator;
import com.ware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 *
 */
public abstract class AbstractGrpcRequestStreamObserver
        extends CallStreamObserver<InteractivePayload>
        implements IRequestStreamCallbackRegistrator {

    protected IRemotingCallStreamObserver callStreamObserver;

    @Override
    public void registryCallback(IRemotingCallStreamObserver value) {
        this.callStreamObserver = value;
    }

    @Override
    public boolean isReady() {
        return callStreamObserver.isReady();
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {
        callStreamObserver.setOnReadyHandler(onReadyHandler);
    }

    @Override
    public void disableAutoInboundFlowControl() {
        callStreamObserver.disableAutoInboundFlowControl();
    }

    @Override
    public void request(int count) {
        callStreamObserver.request(count);
    }

    @Override
    public void setMessageCompression(boolean enable) {
        callStreamObserver.setMessageCompression(enable);
    }

    @Override
    public abstract void onNext(InteractivePayload value);

    @Override
    public void onError(Throwable t) {
        this.callStreamObserver.onError(t);
    }

    @Override
    public void onCompleted() {
        this.callStreamObserver.onCompleted();
    }
}
