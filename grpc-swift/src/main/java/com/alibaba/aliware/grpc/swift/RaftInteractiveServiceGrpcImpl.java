package com.alibaba.aliware.grpc.swift;

import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.alibaba.aliware.swift.proto.InteractiveServiceGrpc;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public class RaftInteractiveServiceGrpcImpl
        extends InteractiveServiceGrpc.InteractiveServiceImplBase {
    private final static InternalLogger logger = InternalLoggerFactory
            .getInstance(RaftInteractiveServiceGrpcImpl.class);

    /**
     * @param responseObserver if you have some payload send to other nodes,you can use
     *                         the responseObserver
     * @return stream observer handler the the nodes request
     */
    @Override
    public StreamObserver<InteractivePayload> requestChannel(
            StreamObserver<InteractivePayload> responseObserver) {

        return new ClientRequestStreamObserver(
                (CallStreamObserver<InteractivePayload>) responseObserver);
    }

    @Override
    public void requestResponse(InteractivePayload request,
                                StreamObserver<InteractivePayload> responseObserver) {

        int eventType = request.getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive raftInteractive = new GrpcRequestResponseInteractive(
                request, (CallStreamObserver) responseObserver);
        RemotingEventDispatcher.getInstance()
                .dispatcher(new ObjectEvent(this, raftInteractive, eventType));
    }

    @Override
    public void requestStream(InteractivePayload request,
                              StreamObserver<InteractivePayload> responseObserver) {
        int eventType = request.getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive raftInteractive = new GrpcRequestStreamInteractive(
                request, (CallStreamObserver) responseObserver);
        RemotingEventDispatcher.getInstance()
                .dispatcher(new ObjectEvent(this, raftInteractive, eventType));
    }
}
