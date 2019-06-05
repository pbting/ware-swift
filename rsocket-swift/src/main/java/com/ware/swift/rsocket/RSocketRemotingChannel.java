package com.ware.swift.rsocket;

import com.google.protobuf.InvalidProtocolBufferException;
import com.ware.swift.core.remoting.IRemotingCallStreamObserver;
import com.ware.swift.core.remoting.IRequestStreamCallbackRegistrator;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannelModel;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;

import java.nio.ByteBuffer;

/**
 *
 */
public class RSocketRemotingChannel extends AbstractRemotingChannelModel {
    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(RSocketRemotingChannel.class);
    private RSocket rSocket;
    private RSocketClientBootstrap clientBootstrap;

    public RSocketRemotingChannel(String addressPort,
                                  RSocketClientBootstrap clientBootstrap, JoinType joinType) {
        super(addressPort, joinType);
        this.clientBootstrap = clientBootstrap;
        rSocket = clientBootstrap.bootstrap();
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isNetUnavailable(Exception e) {
        return this.rSocket.isDisposed();
    }

    @Override
    public InteractivePayload requestResponse(InteractivePayload inBound) {
        InteractivePayload interactivePayload = inBound;

        checkIsBootstrap();

        byte[] data = this.rSocket
                .requestResponse(DefaultPayload.create(interactivePayload.toByteArray()))
                .map(payload -> {
                    ByteBuffer byteBuffer = payload.getData();
                    if (payload.getData().hasArray()) {
                        return byteBuffer.array();
                    }
                    return new byte[]{};
                }).block();

        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        try {
            return builder.mergeFrom(data).build();
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private void checkIsBootstrap() {
        if (this.rSocket.isDisposed()) {
            rSocket = clientBootstrap.bootstrap();
        }
    }

    /**
     * @param inBound
     * @return
     */
    @Override
    public IRequestStreamCallbackRegistrator requestStream(InteractivePayload inBound) {

        this.checkIsBootstrap();
        return (callStreamObserver) -> {
            // 注意: 这里是异步触发的。
            rSocket
                    .requestStream(DefaultPayload.create(inBound.toByteArray()))
                    .map(response -> {
                        ByteBuffer byteBuffer = response.getData();
                        byte[] data = byteBuffer.array();
                        return data;
                    }).doOnNext(responseData -> {
                if (callStreamObserver == null) {
                    log.warn(IRemotingCallStreamObserver.class.getName()
                            + " is not set.");
                    return;
                }
                InteractivePayload.Builder builder = InteractivePayload
                        .newBuilder();
                try {
                    InteractivePayload interactivePayload = builder
                            .mergeFrom(responseData).build();
                    callStreamObserver.onNext(interactivePayload);
                } catch (Exception e) {
                    log.error(
                            "request/stream mergeFrom build cause an exception,",
                            e);
                    callStreamObserver.onError(e);
                }
            }).onTerminateDetach().subscribe();
        };
    }

    @Override
    public void fireAndForget(InteractivePayload inBound) {
    }
}
