package com.alibaba.aliware.core.swift.remoting.channel;

/**
 * 
 */
public interface IRemotingChannelFactory {

	AbstractRemotingChannel newRemotingChannel(String addressPort, String clusterName);
}
