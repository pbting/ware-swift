package com.alibaba.aliware.grpc.swift;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;

/**
 * @param
 */
public abstract class AbstractCallStreamObserver
        extends CallStreamObserver<InteractivePayload>
        implements IInteractive, IIdentifyStreamObserver {

    private String indentifyStream;
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

    public abstract void request(GrpcRequestStreamInteractive raftInteractive);

    public boolean sendPayload(InteractivePayload raftInteractivePayload) {
        if (interactiveStream.isReady()) {
            interactiveStream.onNext(raftInteractivePayload);
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
    public String getIndendity() {
        return indentifyStream;
    }

    @Override
    public void setIndendity(String indendity) {
        this.indentifyStream = indendity;
    }
}
