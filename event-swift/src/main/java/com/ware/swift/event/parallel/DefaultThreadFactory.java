package com.ware.swift.event.parallel;

import com.ware.swift.event.common.Log;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {
	private final static AtomicInteger poolNumber = new AtomicInteger(1);
	final ThreadGroup group;
	final AtomicInteger threadNumber = new AtomicInteger(1);
	final String namePrefix;

	public Thread newThread(Runnable runnable) {
		Thread thread = new Thread(group, runnable, (new StringBuilder()).append(namePrefix).append(threadNumber.getAndIncrement()).toString(), 0L);
		if (thread.isDaemon()){
			thread.setDaemon(false);
		}
		
		if (thread.getPriority() != 5){
			thread.setPriority(5);
		}
		
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.error("proxy-globale-async-executor", e);
			}
		});
		
		return thread;
	}

	public DefaultThreadFactory(String prefix) {
		SecurityManager securitymanager = System.getSecurityManager();
		group = securitymanager == null ? Thread.currentThread().getThreadGroup() : securitymanager.getThreadGroup();
		namePrefix = (new StringBuilder()).append("pool-").append(poolNumber.getAndIncrement()).append("-").append(prefix).append("-thread-").toString();
	}
}