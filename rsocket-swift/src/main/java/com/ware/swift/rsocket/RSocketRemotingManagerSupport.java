package com.ware.swift.rsocket;

import reactor.core.publisher.ReplayProcessor;

import com.ware.swift.core.remoting.AbstractRemotingManager;
import com.ware.swift.core.remoting.channel.IRemotingChannelFactory;
import com.ware.swift.core.remoting.event.local.StartupServerEventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 */
public abstract class RSocketRemotingManagerSupport extends AbstractRemotingManager {

	private IRemotingChannelFactory remotingChannelFactory = new RSocketRemotingChannelFactory(
			this);

	private RSocketServerBootstrap serverBootstrap;

	private Map<String, ReplayProcessor> watchNotification = new ConcurrentHashMap<>();

	@Override
	public IRemotingChannelFactory getRemotingChannelFactory() {

		return remotingChannelFactory;
	}

	@Override
	public <T> void setServer(T server) {
		this.serverBootstrap = (RSocketServerBootstrap) server;
	}

	@Override
	public StartupServerEventListener initStartupServerEventListener() {
		return new RSocketStartupServerEventListener(this);
	}

	@Override
	public <ReplayProcessor> void putIfAbsentStreamReplayProcessor(
			String streamTopicIdentify, ReplayProcessor streamReplayProcessor) {
		if (watchNotification.containsKey(streamTopicIdentify)) {
			return;
		}

		watchNotification.put(streamTopicIdentify,
				(reactor.core.publisher.ReplayProcessor) streamReplayProcessor);
	}
}
