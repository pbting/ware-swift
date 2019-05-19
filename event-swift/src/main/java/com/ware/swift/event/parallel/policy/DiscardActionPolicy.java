package com.ware.swift.event.parallel.policy;

import com.ware.swift.event.common.Log;
import com.ware.swift.event.parallel.action.Action;

import java.util.concurrent.locks.ReentrantLock;

import com.ware.swift.event.parallel.action.IActionQueue;

public class DiscardActionPolicy implements IRejectedActionPolicy{

	@SuppressWarnings("rawtypes")
	@Override
	public void rejectedAction(IActionQueue actionQueue, Action action, ReentrantLock lock) {
		
		Log.debug(String.format("the action for name [%s] is discard", action.getTopicName()));
	}

}
