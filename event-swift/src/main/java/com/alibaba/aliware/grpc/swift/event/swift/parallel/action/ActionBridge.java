package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

public class ActionBridge extends Action{

	private Runnable runnable;
	public ActionBridge(Runnable runnable) {
		super();
		this.runnable = runnable;
	}

	@Override
	public void execute() throws ActionExecuteException {
		if(runnable == null){
			return ;
		}
		
		runnable.run();
	}
	
}
