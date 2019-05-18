package com.alibaba.aliware.rsocket.swift;

/**
 * 
 * @param <T>
 */
public abstract class AbstractBootstrap<T> implements IRSocketBootstrap<T> {
	protected String address;
	protected int port;

	public AbstractBootstrap(String address, int port) {
		this.address = address;
		this.port = port;
	}
}
