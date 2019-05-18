package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.rsocket.swift.handler.RSocketRequestHandlerSupport;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.channel.IRemotingChannelFactory;

import java.util.concurrent.TimeUnit;

/**
 * 这里初始化于 Server 端建立连接
 */
public class RSocketRemotingChannelFactory implements IRemotingChannelFactory {

	private IRemotingManager remotingManager;

	public RSocketRemotingChannelFactory(IRemotingManager remotingManager) {
		this.remotingManager = remotingManager;
	}

	@Override
	public AbstractRemotingChannel newRemotingChannel(String addressPort,
			String clusterName) {
		String[] addressPortArr = addressPort.split("[:]");

		RSocketClientBootstrap clientBootstrap = new RSocketClientBootstrap(
				addressPortArr[0], Integer.valueOf(addressPortArr[1]));

		clientBootstrap
				.setSocketRequestHandlerFactory(new IRSocketRequestHandlerFactory() {
					@Override
					public RSocketRequestHandlerSupport createWithClient() {
						return new RSocketClientSideInboundHandler(
								RSocketRequestHandlerSupport.RSocketRequestHandlerRole.CLIENT);
					}
				});
		while (true) {
			try {
				AbstractRemotingChannel remotingChannel = new RSocketRemotingChannel(
						addressPort, clientBootstrap);
				remotingChannel.setIdentify(remotingManager
						.getChannelIdentify(addressPort + "@" + clusterName));
				return remotingChannel;
			}
			catch (Exception e) {
				System.err.println(addressPort + " is unavliable...");
				try {
					TimeUnit.SECONDS.sleep(1);
				}
				catch (InterruptedException e1) {
				}
			}
		}
	}

}
