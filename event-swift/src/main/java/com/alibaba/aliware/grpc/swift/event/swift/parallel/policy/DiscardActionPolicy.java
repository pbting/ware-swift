package com.alibaba.aliware.grpc.swift.event.swift.parallel.policy;

import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IActionQueue;
import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;

public class DiscardActionPolicy implements IRejectedActionPolicy{

	@SuppressWarnings("rawtypes")
	@Override
	public void rejectedAction(IActionQueue actionQueue, Action action, ReentrantLock lock) {
		
		Log.debug(String.format("the action for name [%s] is discard", action.getTopicName()));
	}

}
