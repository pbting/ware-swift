package com.ware.swift.grpc;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 * @param
 */
public abstract class AbstractCallStreamObserver
        extends CallStreamObserver<InteractivePayload>
        implements IInteractive, IIdentifyStreamObserver {

    private String identifyStream;
    /**
     * if you have some payload to send other nodes,it very useful.
     */
    protected CallStreamObserver<InteractivePayload> interactiveStream;

    public AbstractCallStreamObserver(
            CallStreamObserver<InteractivePayload> interactiveStream) {
        this.interactiveStream = interactiveStream;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {

    }

    @Override
    public void disableAutoInboundFlowControl() {

    }

    @Override
    public void request(int count) {

    }

    @Override
    public void setMessageCompression(boolean enable) {

    }

    public void onCompleted() {

    }

    public void onNext(InteractivePayload value) {
        request(new GrpcRequestStreamInteractive(value, interactiveStream));
    }

    public abstract void request(GrpcRequestStreamInteractive wareSwiftInteractive);

    public boolean sendPayload(InteractivePayload wareSwiftInteractivePayload) {
        if (interactiveStream.isReady()) {
            interactiveStream.onNext(wareSwiftInteractivePayload);
            return true;
        }

        return false;
    }

    /**
     * the client is disconnect will call this method
     *
     * @param t
     */
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public String getIdentify() {
        return identifyStream;
    }

    @Override
    public void setIdentify(String identify) {
        this.identifyStream = identify;
    }
}
