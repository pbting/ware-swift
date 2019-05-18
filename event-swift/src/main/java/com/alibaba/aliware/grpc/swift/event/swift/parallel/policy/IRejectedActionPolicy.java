package com.alibaba.aliware.grpc.swift.event.swift.parallel.policy;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IActionQueue;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;

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
