package com.ware.swift.event.mcache;

/**
 * @param <K>
 * @param <V>
 */
public class DefaultExpireKeyAdaptor<K, V> implements IExpireKeyHandler<K, V> {

    @Override
    public void expire(K key, V value, AbstractConcurrentCache<K, V> cache) {

    }

}
