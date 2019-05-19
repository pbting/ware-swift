package com.ware.swift.grpc;

import com.ware.swift.core.remoting.AbstractRemotingManager;
import com.ware.swift.core.remoting.channel.IRemotingChannelFactory;
import com.ware.swift.core.remoting.event.local.StartupServerEventListener;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public abstract class GrpcRemotingManagerSupport extends AbstractRemotingManager {

    private Server server;

    private IRemotingChannelFactory remotingChannelFactory = new GrpcRemotingChannelFactory(
            this);

    private ConcurrentHashMap<String, StreamObserver> stringStreamObserverConcurrentHashMap = new ConcurrentHashMap<>();

    @Override
    public <T> void setServer(T server) {
        this.server = (Server) server;
    }

    @Override
    public IRemotingChannelFactory getRemotingChannelFactory() {
        return remotingChannelFactory;
    }

    @Override
    public StartupServerEventListener initStartupServerEventListener() {
        return new GrpcStartupServerEventListener(this);
    }

    @Override
    public <V> void putIfAbsentStreamReplayProcessor(String streamTopicIdentify,
                                                     V streamReplayProcessor) {
        if (stringStreamObserverConcurrentHashMap.contains(streamTopicIdentify)) {
            return;
        }

        stringStreamObserverConcurrentHashMap.put(streamTopicIdentify,
                (StreamObserver) streamReplayProcessor);
    }
}
