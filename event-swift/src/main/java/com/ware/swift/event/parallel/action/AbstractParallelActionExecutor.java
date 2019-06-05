package com.ware.swift.event.parallel.action;

import com.ware.swift.event.parallel.policy.DirectlyCallBackPolicy;
import com.ware.swift.event.parallel.policy.IRejectedActionPolicy;
import com.ware.swift.event.parallel.AbstractParallelQueueExecutor;

public abstract class AbstractParallelActionExecutor extends AbstractParallelQueueExecutor implements IParallelActionExecutor{

	private IRejectedActionPolicy rejectedExecutionHandler = new DirectlyCallBackPolicy();
	private int gatingActionQueueSize = Integer.MAX_VALUE;
	
	@Override
	public void registerRejectedActionHandler(int gatingActionQueueSize,IRejectedActionPolicy rejectedExecutionHandler) {
		// 1、
		if (gatingActionQueueSize < 0) {
			gatingActionQueueSize = Integer.MAX_VALUE / 4;
		}
		this.gatingActionQueueSize = gatingActionQueueSize;

		// 2、
		if (rejectedExecutionHandler != null) {
			this.rejectedExecutionHandler = rejectedExecutionHandler;
		}
	}
	
	@Override
	public void trrigerWithRejectActionPolicy(IActionQueue<Action> actionQueue,Action action) {
		if(actionQueue.getQueue().size() > gatingActionQueueSize){
			
			this.rejectedExecutionHandler.rejectedAction(actionQueue, action, actionQueue.getActionQueueLock());
		}else{
			actionQueue.enqueue(action);
		}
	}
}
