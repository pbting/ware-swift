package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 吞吐量优先的行为队列
 * 说明：这里的队列是严格的无阻塞队列。如果这个队列中某个业务室阻塞式的，那么将会导致一个严重的问题就是后面的action 就无法保证顺利执行。
 * 使用场景：无阻塞式行为队列
 * @author pengbingting
 *
 */
public class ParallelActionQueue extends AbstractActionQueue{
	
	//-------------------------------
	private ThreadPoolExecutor executor;
	
	public ParallelActionQueue(ThreadPoolExecutor executor) {
		super(new LinkedList<Action>());
		this.executor = executor;
	}

	public ParallelActionQueue(ThreadPoolExecutor executor, Queue<Action> queue) {
		super(queue);
		this.executor = executor;
	}

	@Override
	public void doExecute(Runnable action) {
		executor.execute(action);
	}
	
	@Override
	public void clear() {
		super.clear();
	}
}
