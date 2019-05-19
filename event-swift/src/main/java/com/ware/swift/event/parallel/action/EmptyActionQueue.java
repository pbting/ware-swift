package com.ware.swift.event.parallel.action;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class EmptyActionQueue implements IActionQueue<Action> {

	@Override
	public IActionQueue<Action> getActionQueue() {
		return this;
	}

	@Override
	public void enqueue(Action t) {
		t.run();
	}

	@Override
	public void dequeue(Action t) {
		// nothing to do
	}

	@Override
	public void clear() {

	}

	@Override
	public Queue<Action> getQueue() {
		return new LinkedList<Action>();
	}

	@Override
	public long getLastActiveTime() {
		return System.currentTimeMillis();
	}

	@Override
	public void doExecute(Runnable action) {
		// TODO Auto-generated method stub

	}

	@Override
	public ReentrantLock getActionQueueLock() {

		return new ReentrantLock();
	}
}
