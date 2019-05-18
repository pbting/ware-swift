package com.ware.swift.event.graph;

import com.ware.swift.event.graph.future.CallableNodeCmd;


public final class GraphRunnable implements Runnable{

	private Node<INodeCommand> dependCommand ;
	private GraphScheduler scheduler ;
	public GraphRunnable(Node<INodeCommand> dependEJob, GraphScheduler scheduler) {
		this.dependCommand = dependEJob ;
		this.scheduler = scheduler ;
	}
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			if(dependCommand.getVal() instanceof RunnableNodeCmd){
				RunnableNodeCmd runnaNodeCmd = (RunnableNodeCmd) dependCommand.getVal();
				runnaNodeCmd.handler();;
			}else if(dependCommand.getVal() instanceof CallableNodeCmd<?>){
				CallableNodeCmd<Object> callableNodeCmd = (CallableNodeCmd<Object>) dependCommand.getVal();
				Object t = callableNodeCmd.call();
				if(callableNodeCmd.getFutureResult() == null){
					throw new IllegalArgumentException("callable node command must set a future node cmd."+ callableNodeCmd.toString());
				}
				callableNodeCmd.getFutureResult().setResult(t);
			}
			this.scheduler.notify(dependCommand);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
