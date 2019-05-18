package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.core.swift.WareCoreSwiftConfig;
import com.alibaba.aliware.rsocket.swift.handler.RSocketRequestHandlerSupport;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.event.local.StartupServerEventListener;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;

/**
 * 启动一个 RSocket 的服务
 */
public class RSocketStartupServerEventListener extends StartupServerEventListener {

	public RSocketStartupServerEventListener(IRemotingManager remotingManager) {
		super(remotingManager);
	}

	@Override
	public void startupServer(WareCoreSwiftConfig raftConfig) {

		RSocketServerBootstrap serverBootstrap = new RSocketServerBootstrap(
				raftConfig.getNodeInformation().getBindAddress(),
				Integer.valueOf(raftConfig.getNodeInformation().getBindPort()));
		remotingManager.setRaftConfig(raftConfig);
		remotingManager.setServer(serverBootstrap);
		serverBootstrap
				.setSocketRequestHandlerFactory(new IRSocketRequestHandlerFactory() {
					@Override
					public RSocketRequestHandlerSupport createWithServer(
							ConnectionSetupPayload setup, RSocket sendingSocket) {
						return new RSocketServerSideInboundHandler(setup, sendingSocket);
					}
				});
		serverBootstrap.bootstrap();
	}
}
