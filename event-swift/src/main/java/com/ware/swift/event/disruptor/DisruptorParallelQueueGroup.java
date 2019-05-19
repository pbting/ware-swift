package com.ware.swift.event.disruptor;

import com.ware.swift.event.parallel.AbstractParallelQueueExecutor;

/**
 * 默认的基于 disruptor 的两层 并行队列来实现
 * @author pengbingting
 *
 */
public class DisruptorParallelQueueGroup extends AbstractParallelQueueExecutor{

	/**
	 * 
	 */
	private DisruptorParallelQueueExecutor bossParallelQueueExecutor ;
	
	/**
	 * 
	 */
	private DisruptorParallelQueueExecutor workerParallelQueueExecutor ;
	
	
	public DisruptorParallelQueueGroup(int bossSize,int bossRingBufferSize,int workerSize,int workerRingBufferSize){
		this.bossParallelQueueExecutor = new DisruptorParallelQueueExecutor(bossSize,bossRingBufferSize);
		this.workerParallelQueueExecutor = new DisruptorParallelQueueExecutor(workerSize, workerRingBufferSize);
	}


	@Override
	public void execute(String topic, Runnable command) {
		
		execute(topic, command, null);
	}


	public void execute(String topic, Runnable command,IRunnableContext runnableContext) {
		
		bossParallelQueueExecutor.execute(topic, new RunnableWapper(topic,command,runnableContext));
	}
	
	@Override
	public void executeOneTime(Runnable command) {
		bossParallelQueueExecutor.executeOneTime(command);
	}


	@Override
	public void registerTopics(String... topics) {
		//nothing to do
	}


	@Override
	public void removeTopic(String topic) {
		//nothing to do
	}


	@Override
	public void execute(Runnable command) {
		
		execute(command.getClass().getName(), command);
	}
	
	public void execute(Runnable command,IRunnableContext runnableContext) {
		
		execute(command.getClass().getName(), command, runnableContext);
	}
	
	private class RunnableWapper implements Runnable{
		private Runnable runnable ;
		private String topic ;
		private IRunnableContext runnableContext = null;
		
		public RunnableWapper(String topic,Runnable runnable,IRunnableContext runnableContext) {
			super();
			this.topic = topic ;
			this.runnable = runnable;
			this.runnableContext = runnableContext;
		}
		
		@Override
		public void run() {
			if(runnableContext != null){
				
				runnableContext.preRunnable();
			}
			workerParallelQueueExecutor.execute(topic, runnable);
		}
	}
	
}
