package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;

/**
 *
 * Leader-Follower 架构下的 初始化 remoting channel 之后的操作
 *
 */
public class LeaderFollowerInitRemotingChannelEventListener
		extends InitRemotingChannelEventListener {

	public LeaderFollowerInitRemotingChannelEventListener(IRemotingManager remotingManager) {
		super(remotingManager);
	}

	@Override
	public void onAfterNewRemotingChannel(AbstractRemotingChannel remotingChannel) {
		remotingManager.getEventLoopGroup().publish(remotingChannel,
				AbstractRemotingManager.SEND_SAY_HELLO_EVENT_TYPE);
	}
}
