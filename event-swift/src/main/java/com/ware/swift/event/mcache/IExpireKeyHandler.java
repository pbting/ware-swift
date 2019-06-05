package com.ware.swift.event.mcache;

public interface IExpireKeyHandler<K, V> {

	void expire(K key, V value, AbstractConcurrentCache<K, V> cache);
}
