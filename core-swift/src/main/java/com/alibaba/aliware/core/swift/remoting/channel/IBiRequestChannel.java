package com.alibaba.aliware.core.swift.remoting.channel;

/**
 * 具有全双工通信能力的 channel.
 */
public interface IBiRequestChannel extends IChannelModule {

	/**
	 * 
	 * @param inBound
	 * @param <OUT_BOUND>
	 * @param <IN_BOUND>
	 * @return
	 */
	<OUT_BOUND, IN_BOUND> OUT_BOUND requestChannel(IN_BOUND inBound);
}
