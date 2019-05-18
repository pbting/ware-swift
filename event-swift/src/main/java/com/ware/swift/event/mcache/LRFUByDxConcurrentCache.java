package com.ware.swift.event.mcache;

import com.ware.swift.event.parallel.IParallelQueueExecutor;

/**
 * 最少使用缓存置换算法的实现latest-seldom use cache
 * 
 * @author pbting
 */
public class LRFUByDxConcurrentCache<K,V> extends LRFUAbstractConcurrentCache<K,V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public LRFUByDxConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity, float loadFactor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
	}

	public LRFUByDxConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity);
	}
	
	public LRFUByDxConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,int initialCapacity) {
		super(cacheTopic, IParallelQueueExecutor, new DefaultExpireKeyAdaptor<K, V>(), initialCapacity);
	}
	
	public LRFUByDxConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor);
	}

	public LRFUByDxConcurrentCache(String cacheTopic,IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic,DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor);
	}
	
	@Override
	public float getFactor(SRUKey sruKey) {
		long currentTimeInterval = (System.nanoTime() - createTime) * 1000 + EFACTOR;
		sruKey.lastFactor += (Float.valueOf(sruKey.lastIntervalTime) / currentTimeInterval) * sruKey.lastIntervalTime;
		// 计算出来的结果太大，会影响均方差的计算导无穷大的情况，因此要进行一个降值的超过
		float Ex = (float) (Math.abs(sruKey.lastFactor * sruKey.lastFactor) / (Math.pow(3, 6)));
		// 迭代计算计算均方差，即响应因子
		sruKey.lastDx += ((sruKey.lastIntervalTime - Ex) * (sruKey.lastIntervalTime - Ex) * (Float.valueOf(sruKey.lastIntervalTime) / currentTimeInterval));
		// 返回他的均方差
		return (float) Math.sqrt(sruKey.lastDx);
	}

}
