package com.ware.swift.core.inter;

import java.nio.ByteBuffer;

/**
 * RSocket Mime type encoding Handler
 *
 * @author leijuan
 */
public interface ObjectEncodingHandler {

	ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0);

	/**
	 * encoding params
	 *
	 * @param args arguments
	 * @return byte buffer
	 * @throws Exception exception
	 */
	ByteBuffer encodingParams(Object[] args);

	/**
	 * decode params, the return value maybe array or single object value
	 *
	 * @param data data
	 * @param targetClasses target classes
	 * @return object array or single object value
	 * @throws Exception exception
	 */
	Object decodeParams(ByteBuffer data, Class<?>... targetClasses);

	/**
	 * encode result
	 *
	 * @param result result
	 * @return byte buffer
	 * @throws Exception exception
	 */
	ByteBuffer encodingResult(Object result);

	/**
	 * decode result
	 *
	 * @param data data
	 * @param targetClass target class
	 * @return result object
	 * @throws Exception exception
	 */
	Object decodeResult(ByteBuffer data, Class<?> targetClass);

	default Object decodeResult(byte[] data, Class<?> targetClass) {
		return decodeResult(ByteBuffer.wrap(data), targetClass);
	}
}
