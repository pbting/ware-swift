package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.IRemotingChannelFactory;
import com.alibaba.aliware.core.swift.remoting.event.local.StartupServerEventListener;
import reactor.core.publisher.ReplayProcessor;

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
