package com.ware.swift.event.mcache;

public interface IMissCacheHandler<K, V> {

	V missCacheHandler(K key);
}
