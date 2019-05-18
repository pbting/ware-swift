package com.alibaba.aliware.grpc.swift.event.swift.mcache;

public interface IMissCacheHandler<K, V> {

	V missCacheHandler(K key);
}
