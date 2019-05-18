package com.alibaba.aliware.core.swift.remoting;

/**
 * 
 */
public interface IRequestStreamCallbackRegistrator {

	/**
	 * 
	 * @param value
	 */
	void registryCallback(
			IRemotingCallStreamObserver value);
}
