package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.core.swift.Information;
import com.alibaba.aliware.core.swift.Information;
import com.alibaba.aliware.core.swift.Information;

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
