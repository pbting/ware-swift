package com.alibaba.aliware.grpc.swift.event.swift.parallel;

import java.util.concurrent.Callable;

import com.alibaba.aliware.grpc.swift.event.swift.common.Log;

public abstract class CallableCommand<V> implements Callable<V>{
	private long createTime ;
	public CallableCommand() {
		this.createTime = System.currentTimeMillis() ;
	}
	public V call() throws Exception {
		long start = System.currentTimeMillis();
		V flag = this.execute();
		long end = System.currentTimeMillis();
		long interval = end - start;
		long leftTime = end - createTime;
		
		if (interval >= 1000 || leftTime >= 1100) {
			Log.warn("execute action : " + this.toString() + ", interval : " + interval + ", leftTime : " + leftTime + ", size : ");
		}
		
		return flag;
	}

	public abstract V execute() ;
}
