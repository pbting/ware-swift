package com.alibaba.aliware.rsocket.swift;

/**
 * 
 */
public abstract class RSocketBootstrapSupport<T> extends AbstractBootstrap<T> {

	protected IRSocketRequestHandlerFactory socketRequestHandlerFactory;

	public RSocketBootstrapSupport(String address, int port) {
		super(address, port);
	}

	public IRSocketRequestHandlerFactory getSocketRequestHandlerFactory() {
		return socketRequestHandlerFactory;
	}

	public void setSocketRequestHandlerFactory(
			IRSocketRequestHandlerFactory socketRequestHandlerFactory) {
		this.socketRequestHandlerFactory = socketRequestHandlerFactory;
	}
}
