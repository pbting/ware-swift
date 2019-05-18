package com.ware.swift.event.parallel.policy;

import com.ware.swift.event.parallel.action.Action;
import com.ware.swift.event.parallel.action.IActionQueue;

import java.util.concurrent.locks.ReentrantLock;

public interface IRejectedActionPolicy {

	/**
	 * 
	 * @param actionQueue
	 * @param action
	 * @param lock
	 */
	@SuppressWarnings("rawtypes")
	void rejectedAction(IActionQueue actionQueue, Action action, ReentrantLock lock);
}
