package com.ware.swift.event.mcache;

import com.ware.swift.event.parallel.IParallelQueueExecutor;

/**
 * 最近最少使用缓存置换算法的实现latest-seldom use cache
 * 
 * @author pbting
 */
public class LRFUByExConcurrentCache<K,V> extends LRFUAbstractConcurrentCache<K,V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LRFUByExConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity, float loadFactor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
	}


	public LRFUByExConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity);
	}


	public LRFUByExConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor);
	}

	public LRFUByExConcurrentCache(String cacheTopic,IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic,DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor);
	}

	/**
	 * 这部分代码需要同步
	 */
	@Override
	public float getFactor(SRUKey sruKey) {
		long currentTimeInterval = (System.nanoTime() - createTime) + EFACTOR;
		sruKey.lastFactor += ((Float.valueOf(sruKey.lastIntervalTime) / currentTimeInterval) * sruKey.lastIntervalTime);
		return Math.abs(Math.abs(sruKey.lastFactor) - (float) Math.pow(sruKey.count, 3.2));
	}
}
