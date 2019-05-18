package com.alibaba.aliware.grpc.swift.event.swift.parallel.policy;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IActionQueue;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IActionQueue;

public class DiscardOldestActionPolicy implements IRejectedActionPolicy{

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void rejectedAction(IActionQueue actionQueue, Action action, ReentrantLock lock) {
		final Queue<Action> queue = actionQueue.getQueue();
		try {
			lock.lock();
			queue.poll();
		} finally {
			lock.unlock();
		}
		actionQueue.enqueue(action);
	}

}
