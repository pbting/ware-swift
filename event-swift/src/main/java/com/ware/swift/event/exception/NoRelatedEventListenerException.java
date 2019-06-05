package com.ware.swift.event.exception;

/**
 * 当使用 LPIPublisher时，如果 一个 有效的 event topic 没有关联一个 event group,则抛出这个异常 
 * @author pengbingting
 *
 */
public class NoRelatedEventListenerException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoRelatedEventListenerException() {
		super();
	}

	public NoRelatedEventListenerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoRelatedEventListenerException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoRelatedEventListenerException(String message) {
		super(message);
	}

	public NoRelatedEventListenerException(Throwable cause) {
		super(cause);
	}

}
