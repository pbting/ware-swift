package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.core.swift.remoting.IRemotingCallStreamObserver;
import com.alibaba.aliware.core.swift.remoting.IRequestStreamCallbackRegistrator;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannelModel;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class RSocketRemotingChannel extends AbstractRemotingChannelModel {
    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(RSocketRemotingChannel.class);
    private final AtomicReference<RSocket> rSocketReference = new AtomicReference<>();
    private RSocketClientBootstrap clientBootstrap;

    public RSocketRemotingChannel(String addressPort,
                                  RSocketClientBootstrap clientBootstrap) {
        super(addressPort);
        this.clientBootstrap = clientBootstrap;
        RSocket rSocket = clientBootstrap.bootstrap();
        rSocketReference.set(rSocket);
    }

    @Override
    public boolean isNetUnavailable(Exception e) {
        return this.rSocketReference.get().isDisposed();
    }

    @Override
    public InteractivePayload requestResponse(InteractivePayload inBound) {
        InteractivePayload interactivePayload = inBound;

        checkIsBootstrap();

        byte[] data = this.rSocketReference.get()
                .requestResponse(DefaultPayload.create(interactivePayload.toByteArray()))
                .map(payload -> {
                    ByteBuffer byteBuffer = payload.getData();
                    return byteBuffer.array();
                }).block();

        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        try {
            return builder.mergeFrom(data).build();
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private void checkIsBootstrap() {
        if (this.rSocketReference.get().isDisposed()) {
            rSocketReference.set(clientBootstrap.bootstrap());
        }
    }

    private static final AtomicLong syncDataSize = new AtomicLong();

    /**
     * @param inBound
     * @return
     */
    @Override
    public IRequestStreamCallbackRegistrator requestStream(InteractivePayload inBound) {

        this.checkIsBootstrap();
        return (callStreamObserver) -> {
            // 注意: 这里是异步触发的。
            rSocketReference.get()
                    .requestStream(DefaultPayload.create(inBound.toByteArray()))
                    .map(response -> {
                        ByteBuffer byteBuffer = response.getData();
                        byte[] data = byteBuffer.array();
                        return data;
                    }).doOnNext(responseData -> {
                System.err.println("\treceive sync data size[request/stream]:"
                        + syncDataSize.incrementAndGet());
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
