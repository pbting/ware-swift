package com.ware.swift.event.object;

import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.graph.future.FutureResult;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @author pengbingting
 *
 * @param <V>
 */
public class FutureObjectEvent<V> extends ObjectEvent<V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private FutureResult<Object> futureResult;

	public FutureObjectEvent(Object source, V value, int eventType) {
		super(source, value, eventType);
		this.futureResult = new FutureResult<>();
	}

	public void setResult(Object result) {
		this.futureResult.setResult(result);
	}

	public <T> Object getResult() {

		return this.futureResult.getResult();
	}

	public <T> Object getResult(int timeOut) {

		return this.futureResult.getResult(timeOut);
	}

	public <T> Object getResult(int timeOut, TimeUnit timeUnit) {

		return this.futureResult.getResult(timeUnit.toMillis(timeOut));
	}

	public static void main(String[] args) {
		System.err.println(TimeUnit.SECONDS.toMillis(1));
	}
}
