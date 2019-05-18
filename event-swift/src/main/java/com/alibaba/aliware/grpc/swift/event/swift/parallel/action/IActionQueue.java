package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public interface IActionQueue<T extends Runnable> {

	IActionQueue<T> getActionQueue();

	void enqueue(T t);

	void dequeue(T t);

	void clear();

	Queue<T> getQueue();

	/**
	 * 得到最后一次活动的时间。会定时的清理一些长时间未活动的队列。
	 */
	long getLastActiveTime();

	/**
	 * 
	 * @param runnable
	 */
	void doExecute(Runnable runnable);

	/**
	 * 
	 * @return
	 */
	ReentrantLock getActionQueueLock();
}
