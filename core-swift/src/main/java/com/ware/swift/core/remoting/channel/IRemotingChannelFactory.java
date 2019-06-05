package com.ware.swift.core.remoting.channel;

/**
 *
 */
public interface IRemotingChannelFactory {

    AbstractRemotingChannel newRemotingChannel(String addressPort, String clusterName, AbstractRemotingChannel.JoinType joinType);
}
