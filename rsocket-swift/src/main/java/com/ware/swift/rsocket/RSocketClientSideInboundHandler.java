package com.ware.swift.rsocket;

import com.ware.swift.rsocket.handler.RSocketRequestHandlerSupport;
import org.reactivestreams.Publisher;

import io.rsocket.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * RSocket 客户端接收服务端发送过来的数据流处理入口
 */
public class RSocketClientSideInboundHandler extends RSocketRequestHandlerSupport {

	public RSocketClientSideInboundHandler(RSocketRequestHandlerRole role) {
		super(role);
	}

	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		return super.fireAndForget(payload);
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		return super.requestStream(payload);
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		return super.requestResponse(payload);
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return super.requestChannel(payloads);
	}
}
