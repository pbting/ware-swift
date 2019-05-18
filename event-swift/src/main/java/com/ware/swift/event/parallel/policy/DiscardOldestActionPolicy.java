package com.ware.swift.event.parallel.policy;

import com.ware.swift.event.parallel.action.Action;
import com.ware.swift.event.parallel.action.IActionQueue;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

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
