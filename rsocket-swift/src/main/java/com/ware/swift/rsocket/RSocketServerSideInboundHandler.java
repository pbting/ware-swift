package com.ware.swift.rsocket;

import com.google.protobuf.InvalidProtocolBufferException;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.proto.InteractivePayload;
import com.ware.swift.rsocket.handler.RSocketRequestHandlerSupport;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

/**
 * RSocket Server/Client 端接收客户端请求的入口。
 * <p>
 * 就相当于 Grpc 的
 */
public class RSocketServerSideInboundHandler extends RSocketRequestHandlerSupport {

    public RSocketServerSideInboundHandler(ConnectionSetupPayload setupPayload,
                                           RSocket peerRsocket) {
        super(setupPayload, peerRsocket);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        // how to asyn processor for request/response
        final ReplayProcessor replayProcessor = ReplayProcessor.cacheLast();
        try {
            InteractivePayload receivePayload = InteractivePayload.newBuilder()
                    .mergeFrom(payload.getData().array()).build();
            IInteractive rSocketRaftInteractive = new RSocketInteractiveImpl(
                    receivePayload, replayProcessor);
            // 这里异步处理
            RemotingEventDispatcher.getInstance().publish(rSocketRaftInteractive,
                    receivePayload.getEventType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Mono.create(payloadMonoSink -> replayProcessor.subscribe(result -> {
            try {
                payloadMonoSink.success(DefaultPayload.create((byte[]) result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return super.requestChannel(payloads);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        try {
            InteractivePayload receivePayload = InteractivePayload.newBuilder()
                    .mergeFrom(payload.getData().array()).build();
            // 1. prepare the ReplayProcessor
            final ReplayProcessor replayProcessor = ReplayProcessor
                    .create(Integer.MAX_VALUE);
            // 2.
            Flux<Payload> streamFlux = Flux
                    .create(payloadFluxSink -> replayProcessor.subscribe(response -> {
                        try {
                            payloadFluxSink.next(DefaultPayload
                                    .create(DefaultPayload.create((byte[]) response)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
            // 3. dispatcher
            IInteractive rSocketRaftInteractive = new RSocketInteractiveImpl(
                    receivePayload, replayProcessor);
            RemotingEventDispatcher.getInstance().publish(rSocketRaftInteractive,
                    receivePayload.getEventType());
            return streamFlux;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return Flux.empty();
        }
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        return super.fireAndForget(payload);
    }
}
