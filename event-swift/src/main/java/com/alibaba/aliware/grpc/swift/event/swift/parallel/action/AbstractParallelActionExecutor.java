package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.AbstractParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.policy.DirectlyCallBackPolicy;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.policy.IRejectedActionPolicy;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.AbstractParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.policy.DirectlyCallBackPolicy;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.policy.IRejectedActionPolicy;

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
