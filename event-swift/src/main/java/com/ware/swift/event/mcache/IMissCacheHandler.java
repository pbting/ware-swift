package com.ware.swift.event.mcache;

/**
 * @param <K>
 * @param <V>
 */
public interface IMissCacheHandler<K, V> {

    V missCacheHandler(K key);
}
