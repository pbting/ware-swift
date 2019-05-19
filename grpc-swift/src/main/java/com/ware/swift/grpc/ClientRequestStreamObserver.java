package com.ware.swift.grpc;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.grpc.stub.CallStreamObserver;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 服务端异步请求的处理入口。即客户端对服务端请求，服务端的处理入口
 */
public class ClientRequestStreamObserver extends AbstractCallStreamObserver
        implements IInteractive {
    private final static InternalLogger logger = InternalLoggerFactory
            .getInstance(ClientRequestStreamObserver.class);

    public ClientRequestStreamObserver(
            CallStreamObserver<InteractivePayload> responseStreamObserver) {
        super(responseStreamObserver);
    }

    /**
     * The Entry of Client Request
     *
     * @param raftInteractive
     */
    @Override
    public void request(GrpcRequestStreamInteractive raftInteractive) {
        int eventType = raftInteractive.getInteractivePayload().getEventType();
        logger.debug("receive client request with the event type :" + eventType);
        RemotingEventDispatcher.getInstance()
                .dispatcher(new ObjectEvent(this, raftInteractive, eventType));
    }

    @Override
    public AbstractCallStreamObserver getAbstractCallStreamObserver() {
        return this;
    }
}
