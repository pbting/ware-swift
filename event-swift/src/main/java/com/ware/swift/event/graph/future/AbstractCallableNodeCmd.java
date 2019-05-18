package com.ware.swift.event.graph.future;

public abstract class AbstractCallableNodeCmd<T> implements CallableNodeCmd<T> {

	private FutureResult<T> futureResult ;
	public AbstractCallableNodeCmd() {
		this.futureResult = new FutureResult<T>();
	}
	
	public FutureResult<T> getFutureResult() {
		return this.futureResult;
	}

	
	public T get() {
		return this.futureResult.getResult();
	}
}
