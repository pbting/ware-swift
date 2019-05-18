package com.alibaba.aliware.grpc.swift.event.swift.object;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.graph.future.FutureResult;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

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
