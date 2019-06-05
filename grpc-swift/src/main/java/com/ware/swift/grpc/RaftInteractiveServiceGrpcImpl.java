package com.ware.swift.grpc;

import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.ware.swift.proto.InteractiveServiceGrpc;
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
        AbstractGrpcInteractive wareSwiftInteractive = new GrpcRequestResponseInteractive(
                request, (CallStreamObserver) responseObserver);
        RemotingEventDispatcher.getInstance()
                .dispatcher(new ObjectEvent(this, wareSwiftInteractive, eventType));
    }

    @Override
    public void requestStream(InteractivePayload request,
                              StreamObserver<InteractivePayload> responseObserver) {
        int eventType = request.getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        AbstractGrpcInteractive wareSwiftInteractive = new GrpcRequestStreamInteractive(
                request, (CallStreamObserver) responseObserver);
        RemotingEventDispatcher.getInstance()
                .dispatcher(new ObjectEvent(this, wareSwiftInteractive, eventType));
    }
}
