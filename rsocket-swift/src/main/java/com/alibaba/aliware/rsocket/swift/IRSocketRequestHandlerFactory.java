package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.rsocket.swift.handler.RSocketRequestHandlerSupport;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;

/**
 * 
 */
public interface IRSocketRequestHandlerFactory {

	default RSocketRequestHandlerSupport createWithServer(ConnectionSetupPayload setup,
			RSocket sendingSocket) {
		return new RSocketRequestHandlerSupport(setup, sendingSocket);
	}

	default RSocketRequestHandlerSupport createWithClient() {

		return new RSocketRequestHandlerSupport(
				RSocketRequestHandlerSupport.RSocketRequestHandlerRole.CLIENT);
	}

	class DefaultRSocketRequestHandlerFactory implements IRSocketRequestHandlerFactory {
	}
}
