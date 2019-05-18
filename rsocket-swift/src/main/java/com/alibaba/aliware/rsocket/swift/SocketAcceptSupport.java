package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.rsocket.swift.handler.RSocketRequestHandlerSupport;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 注意：这里简单是是用来 第一次连接上来的 rsocket 作为返回值。实际情况需要根据每个客户端的连接单独做处理。
 */
public class SocketAcceptSupport implements SocketAcceptor {

	private List<IRSocketAcceptorFilter> acceptorFilters = new ArrayList<>();
	private IRSocketRequestHandlerFactory rSocketRequestHandlerFactory;

	public SocketAcceptSupport(List<IRSocketAcceptorFilter> acceptorFilters) {
		this(acceptorFilters,
				new IRSocketRequestHandlerFactory.DefaultRSocketRequestHandlerFactory());
	}

	public SocketAcceptSupport(List<IRSocketAcceptorFilter> acceptorFilters,
			IRSocketRequestHandlerFactory rSocketRequestHandlerFactory) {
		this.acceptorFilters = acceptorFilters;
		this.rSocketRequestHandlerFactory = rSocketRequestHandlerFactory;
	}

	@Override
	public Mono<RSocket> accept(final ConnectionSetupPayload setup,
			final RSocket sendingSocket) {
		// if Successful authentication
		return Flux.fromIterable(acceptorFilters)
				.all(acceptorFilters -> acceptorFilters.accept(setup, sendingSocket))
				.defaultIfEmpty(true).map(filterResult -> {
					if (!filterResult) {
						return null;
					}
					RSocketRequestHandlerSupport rSocketRequestHandler = rSocketRequestHandlerFactory
							.createWithServer(setup, sendingSocket);
					return rSocketRequestHandler;
				});
	}

}