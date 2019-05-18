
package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

public class ActionExecuteException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ActionExecuteException() {
	}
	public ActionExecuteException(String message){
		super(message);
	}
}
