package com.ware.swift.core;

/**
 * 
 */
public class WareSwiftException extends Exception {

	public WareSwiftException() {
	}

	public WareSwiftException(String message) {
		super(message);
	}

	public WareSwiftException(String message, Throwable cause) {
		super(message, cause);
	}

	public WareSwiftException(Throwable cause) {
		super(cause);
	}

	public WareSwiftException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
