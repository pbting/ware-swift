package com.alibaba.aliware.rsocket.swift;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;

/**
 * rsocket acceptor filter
 *
 * @author leijuan
 */
public interface IRSocketAcceptorFilter {

	boolean accept(ConnectionSetupPayload setup, RSocket sendingSocket);
}
