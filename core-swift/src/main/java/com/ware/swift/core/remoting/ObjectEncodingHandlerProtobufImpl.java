package com.ware.swift.core.remoting;

import com.ware.swift.core.inter.ObjectEncodingHandler;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.nio.ByteBuffer;

/**
 * Object protobuf encoding
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class ObjectEncodingHandlerProtobufImpl implements ObjectEncodingHandler {

	@Override
	public ByteBuffer encodingParams(Object[] args) {
		if (args != null && args.length == 1) {
			return encodingResult(args[0]);
		}
		return EMPTY_BUFFER;
	}

	@Override
	public Object decodeParams(ByteBuffer data, Class<?>... targetClasses) {
		if (data.capacity() >= 1 && targetClasses != null && targetClasses.length == 1) {
			return decodeResult(data, targetClasses[0]);
		}
		return null;
	}

	@Override
	public ByteBuffer encodingResult(Object result) {
		if (result != null) {
			LinkedBuffer buffer = LinkedBuffer.allocate(256);
			Schema schema = RuntimeSchema.getSchema(result.getClass());
			return ByteBuffer.wrap(ProtostuffIOUtil.toByteArray(result, schema, buffer));
		}
		return EMPTY_BUFFER;
	}

	@Override

	public Object decodeResult(ByteBuffer data, Class<?> targetClass) {
		if (data.capacity() >= 1 && targetClass != null) {
			Schema schema = RuntimeSchema.getSchema(targetClass);
			Object object = schema.newMessage();
			ProtostuffIOUtil.mergeFrom(data.array(), object, schema);
			return object;
		}
		return null;
	}

	@Override
	public Object decodeResult(byte[] data, Class<?> targetClass) {
		if (data.length >= 1 && targetClass != null) {
			Schema schema = RuntimeSchema.getSchema(targetClass);
			Object object = schema.newMessage();
			ProtostuffIOUtil.mergeFrom(data, object, schema);
			return object;
		}
		return null;
	}
}
