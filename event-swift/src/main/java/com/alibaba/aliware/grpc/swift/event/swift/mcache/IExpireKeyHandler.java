package com.alibaba.aliware.grpc.swift.event.swift.mcache;

public interface IExpireKeyHandler<K, V> {

	void expire(K key, V value, AbstractConcurrentCache<K, V> cache);
}
