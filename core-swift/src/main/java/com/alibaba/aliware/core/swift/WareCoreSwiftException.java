package com.alibaba.aliware.core.swift;

/**
 * 
 */
public class WareCoreSwiftException extends Exception {

	public WareCoreSwiftException() {
	}

	public WareCoreSwiftException(String message) {
		super(message);
	}

	public WareCoreSwiftException(String message, Throwable cause) {
		super(message, cause);
	}

	public WareCoreSwiftException(Throwable cause) {
		super(cause);
	}

	public WareCoreSwiftException(String message, Throwable cause, boolean enableSuppression,
								  boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
