package com.ware.swift.event.mcache;

public class NoMissCacheHandlerException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoMissCacheHandlerException() {
		super();
	}

	public NoMissCacheHandlerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoMissCacheHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoMissCacheHandlerException(String message) {
		super(message);
	}

	public NoMissCacheHandlerException(Throwable cause) {
		super(cause);
	}

}
