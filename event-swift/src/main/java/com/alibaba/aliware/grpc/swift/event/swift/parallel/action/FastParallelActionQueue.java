package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于响应式优先的并行队列。相对于ParallelActionQueue 的主要区别就是减少数据在多个不同的线程之间的切换
 * @author pengbingting
 *
 */
public class FastParallelActionQueue extends AbstractActionQueue{
	
	private ThreadPoolExecutor signelThreadExecutor ;
	public FastParallelActionQueue(ThreadPoolExecutor signelThreadExecutor,Queue<Action> queue) {
		super(queue);
		if(signelThreadExecutor.getCorePoolSize() != 1){
			signelThreadExecutor.setCorePoolSize(1);
			signelThreadExecutor.setMaximumPoolSize(1);
			signelThreadExecutor.setKeepAliveTime(0, TimeUnit.MILLISECONDS);
		}
		
		this.signelThreadExecutor = signelThreadExecutor;
	}

	public FastParallelActionQueue(ThreadPoolExecutor signelThreadExecutor) {
		super(new LinkedList<Action>());
		if(signelThreadExecutor.getCorePoolSize() != 1){
			signelThreadExecutor.setCorePoolSize(1);
			signelThreadExecutor.setMaximumPoolSize(1);
			signelThreadExecutor.setKeepAliveTime(0, TimeUnit.MILLISECONDS);
		}
		
		this.signelThreadExecutor = signelThreadExecutor;
	}
	
	@Override
	public void doExecute(Runnable action) {
		if(signelThreadExecutor.getQueue().isEmpty()){
			signelThreadExecutor.execute(action);
		}else{
			signelThreadExecutor.getQueue().offer(action);
		}
	}
}
