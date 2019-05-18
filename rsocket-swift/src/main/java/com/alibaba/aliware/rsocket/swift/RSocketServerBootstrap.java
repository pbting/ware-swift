package com.alibaba.aliware.rsocket.swift;

import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.tcp.TcpServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class RSocketServerBootstrap extends RSocketBootstrapSupport<SocketAcceptor> {

	private final static Logger log = Logger
			.getLogger(RSocketServerBootstrap.class.getCanonicalName());

	private ConnectionObserver connectionObserver = new ConnectionObserver() {
		@Override
		public void onStateChange(Connection connection, State newState) {
			if (connectEventListener == null) {
				return;
			}

			switch (newState.toString()) {
			case "[connected]":
				connectEventListener.onConnected(connection);
				break;
			case "[acquired]":
				connectEventListener.onAcquired(connection);
				break;
			case "[configured]":
				connectEventListener.onConfigured(connection);
				break;
			case "[released]":
				connectEventListener.onReleased(connection);
				break;
			case "[disconnecting]":
				connectEventListener.onDisconnecting(connection);
				break;
			}
		}
	};

	protected List<IRSocketAcceptorFilter> rSocketAcceptorFilters = new ArrayList<>();

	private IRSocketConnectEventListener connectEventListener;

	public RSocketServerBootstrap(String address, int port) {
		super(address, port);
	}

	@Override
	public SocketAcceptor bootstrap() {
		try {
			SocketAcceptor socketAcceptor = new SocketAcceptSupport(
					rSocketAcceptorFilters, socketRequestHandlerFactory);
			final RSocketFactory.ServerRSocketFactory serverRSocketFactory = RSocketFactory
					.receive();

			final TcpServer server = TcpServer.create().host(address).port(port);
			final AtomicReference<TcpServer> observerTcpServer = new AtomicReference<>();
			observerTcpServer.set(server.observe(connectionObserver));

			TcpServerTransport tcpServerTransport;
			if (observerTcpServer.get() != null) {
				tcpServerTransport = TcpServerTransport.create(observerTcpServer.get());
			}
			else {
				tcpServerTransport = TcpServerTransport.create(server);
			}
			serverRSocketFactory.acceptor(socketAcceptor).transport(tcpServerTransport)
					.start().onTerminateDetach().subscribe();
			return socketAcceptor;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<IRSocketAcceptorFilter> getrSocketAcceptorFilters() {
		return rSocketAcceptorFilters;
	}

	public void setRSocketConnectEventListener(
			IRSocketConnectEventListener connectEventListener) {
		this.connectEventListener = connectEventListener;
	}

	public void setrSocketAcceptorFilters(
			List<IRSocketAcceptorFilter> rSocketAcceptorFilters) {
		this.rSocketAcceptorFilters = rSocketAcceptorFilters;
	}

}