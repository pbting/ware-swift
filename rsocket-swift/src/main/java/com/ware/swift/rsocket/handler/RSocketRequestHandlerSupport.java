package com.ware.swift.rsocket.handler;

import com.ware.swift.rsocket.IRSocketRequestInitializer;
import org.reactivestreams.Publisher;

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 */
public class RSocketRequestHandlerSupport extends AbstractRSocket
		implements IRSocketRequestInitializer {

	public enum RSocketRequestHandlerRole {
		SERVER, CLIENT, NONE,
	}

	protected RSocketRequestHandlerRole role;

	protected ConnectionSetupPayload setupPayload;
	protected RSocket peerRsocket;

	public RSocketRequestHandlerSupport(RSocketRequestHandlerRole role) {
		this(role, null, null);
	}

	public RSocketRequestHandlerSupport(ConnectionSetupPayload setupPayload,
			RSocket peerRsocket) {

		this(RSocketRequestHandlerRole.NONE, setupPayload, peerRsocket);
	}

	public RSocketRequestHandlerSupport(RSocketRequestHandlerRole role,
			ConnectionSetupPayload setupPayload, RSocket peerRsocket) {
		this.role = role;
		this.setupPayload = setupPayload;
		this.peerRsocket = peerRsocket;
		initializer();
	}

	private AbstractRequestChannelHandler requestChannelHandler;

	@Override
	public void initializer() {
		// nothing to do
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		if (requestChannelHandler != null) {
			Flux.from(payloads).subscribe(requestChannelHandler::processPayload);
			return Flux.from(requestChannelHandler);
		}
		else {
			return super.requestChannel(payloads);
		}
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
	public Mono<Void> fireAndForget(Payload payload) {
		return Mono.empty();
	}

	public void setRequestChannelHandler(
			AbstractRequestChannelHandler requestChannelHandler) {
		this.requestChannelHandler = requestChannelHandler;
	}

	public RSocket getPeerRsocket() {
		return peerRsocket;
	}
}
