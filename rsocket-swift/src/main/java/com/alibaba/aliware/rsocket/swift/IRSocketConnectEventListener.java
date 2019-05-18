package com.alibaba.aliware.rsocket.swift;

import reactor.netty.Connection;

import java.util.EventListener;

/**
 * 
 */
public interface IRSocketConnectEventListener extends EventListener {

	/**
	 * 
	 * @param connection
	 */
	default void onConnected(Connection connection) {
	}

	/**
	 * 
	 * @param connection
	 */
	default void onConfigured(Connection connection) {
	}

	/**
	 * 
	 * @param connection
	 */
	default void onAcquired(Connection connection) {
	}

	/**
	 * 
	 * @param connection
	 */
	default void onReleased(Connection connection) {
	}

	/**
	 * 
	 * @param connection
	 */
	default void onDisconnecting(Connection connection) {
	}
}
