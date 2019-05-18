package com.ware.swift.event.parallel.action;

import com.ware.swift.event.parallel.IParallelQueueExecutor;
import com.ware.swift.event.parallel.policy.IRejectedActionPolicy;

/**
 * 
 */
public interface IParallelActionExecutor extends IParallelQueueExecutor {

	/**
	 * 
	 * @param queueTopic
	 * @param action
	 */
	void enParallelAction(String queueTopic, Action action);

	/**
	 * 
	 * @param queueTopic
	 */
	void removeParallelAction(String queueTopic);

	/**
	 * 
	 * @param newCorePoolSize
	 * @param newMaxiPoolSize
	 */
	void adjustPoolSize(int newCorePoolSize, int newMaxiPoolSize);

	/**
	 * 
	 * @param action
	 */
	void executeOneTimeAction(Action action);

	/**
	 * 
	 * @param gatingActionQueueSize 门控 action 队列的大小。当超过这个大小的时候，会触发
	 *     RejectedExecutionHandler
	 * @param rejectedExecutionHandler 当超过这个大小的时候的 rejected execution handler
	 */
	void registerRejectedActionHandler(int gatingActionQueueSize,
			IRejectedActionPolicy rejectedExecutionHandler);

	/**
	 * 
	 */
	void trrigerWithRejectActionPolicy(IActionQueue<Action> actionQueue, Action action);

}
