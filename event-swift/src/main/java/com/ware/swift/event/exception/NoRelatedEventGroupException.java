package com.ware.swift.event.exception;

/**
 * 当使用 LPIPublisher时，如果 一个 有效的 event topic 没有关联一个 event group,则抛出这个异常 
 * @author pengbingting
 *
 */
public class NoRelatedEventGroupException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoRelatedEventGroupException() {
		super();
	}

	public NoRelatedEventGroupException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoRelatedEventGroupException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoRelatedEventGroupException(String message) {
		super(message);
	}

	public NoRelatedEventGroupException(Throwable cause) {
		super(cause);
	}

}
