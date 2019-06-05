package com.ware.swift.event.mcache;

import com.ware.swift.event.parallel.IParallelQueueExecutor;

public class LRFUConcurrentCache<K,V> extends LRFUAbstractConcurrentCache<K,V> {

	/**
	 * 基于这种的实现只考虑最后两次的访问时间差，记忆无记忆的最近最少缓存置换策略。
	 */
	private static final long serialVersionUID = 1L;

	public LRFUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity, float loadFactor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
	}


	public LRFUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity);
	}


	public LRFUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor);
	}

	public LRFUConcurrentCache(String cacheTopic,IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic,DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor);
	}

	@Override
	public float getFactor(SRUKey sruKey) {

		return System.nanoTime() - sruKey.lastAccessTime + EFACTOR;
	}

}
