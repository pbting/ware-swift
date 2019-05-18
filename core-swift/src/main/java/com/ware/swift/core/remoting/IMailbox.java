package com.ware.swift.core.remoting;

import com.ware.swift.core.Information;

import java.util.concurrent.TimeUnit;

/**
 * 
 */
public interface IMailbox<V> extends Information {

	/**
	 * 
	 * @param value
	 */
	void producer(V value);

	/**
	 * 
	 * @param timeout
	 * @param timeUnit
	 * @return
	 */
	V consumer(int timeout, TimeUnit timeUnit);
}
