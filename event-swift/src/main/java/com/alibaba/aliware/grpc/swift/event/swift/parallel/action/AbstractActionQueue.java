package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.lmax.disruptor.Sequence;
import com.alibaba.aliware.grpc.swift.event.swift.common.Log;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基本队列的实现
 * @author pengbingting
 *
 */
public abstract class AbstractActionQueue implements IActionQueue<Action> {

	protected Queue<Action> queue;
	protected ReentrantLock lock = new ReentrantLock();
	protected Sequence createTime = new Sequence(System.currentTimeMillis());
	protected Sequence lastActiveTime = new Sequence(System.currentTimeMillis());

	public AbstractActionQueue(Queue<Action> queue) {
		super();
		this.queue = queue;
	}

	public IActionQueue<Action> getActionQueue() {
		return this;
	}

	public Queue<Action> getQueue() {
		return queue;
	}

	/**
	 * 进队列，这个 action 会被执行的条件是 队列中没有其他的 action 在执行。否则在队列等待执行
	 */
	public void enqueue(Action action) {
		int queueSize = 0;
		if (action.getActionQueue() == null) {
			action.setActionQueue(this);
		}
		lock.lock();
		try {
			queue.add(action);
			queueSize = queue.size();
		}
		finally {
			lock.unlock();
		}
		if (queueSize == 1) {
			doExecute(action);
		}
	}

	public void dequeue(Action action) {
		lastActiveTime.set(System.currentTimeMillis());
		Action nextAction = null;
		int queueSize = 0;
		String tmpString = null;
		lock.lock();
		try {
			queueSize = queue.size();
			Action temp = queue.remove();
			if (temp != action) {
				tmpString = temp.toString();
			}
			if (queueSize != 0) {
				nextAction = queue.peek();
			}
		}
		finally {
			lock.unlock();
		}

		if (nextAction != null) {
			doExecute(nextAction);
		}

		if (queueSize == 0) {
			Log.debug("queue.size() is 0.");
		}
		if (tmpString != null) {
			Log.debug("action queue error. temp " + tmpString + ", action : "
					+ action.toString());
		}
	}

	public abstract void doExecute(Runnable action);

	public void clear() {
		lock.lock();
		try {
			queue.clear();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public long getLastActiveTime() {

		return lastActiveTime.get();
	}

	@Override
	public ReentrantLock getActionQueueLock() {

		return this.lock;
	}
}
