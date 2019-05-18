/*
 * Copyright (c) 2002-2003 by OpenSymphony All rights reserved.
 */
package com.alibaba.aliware.grpc.swift.event.swift.mcache;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.alibaba.aliware.grpc.swift.event.swift.disruptor.DisruptorParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.disruptor.DisruptorParallelQueueExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractConcurrentCache<K, V> extends ConcurrentHashMap<K, V>
		implements IHighCacheMap<K, V> {

	public final int ITEM_PUT_OP = 1 << 0;
	public final int ITEM_RETRIVE_OP = 1 << 1;
	public final int ITEM_REMOVE_OP = 1 << 2;

	private MCacheManager<K, V> cacheManager;
	private IExpireKeyHandler<K, V> iExpireKeyAdaptor;
	private IMissCacheHandler<K, V> missCacheHandler = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(Log.class);
	/**
	 * Use memory cache or not.
	 */
	protected boolean memoryCaching = true;

	/**
	 * Use unlimited disk caching.
	 */
	protected boolean unlimitedDiskCache = false;

	/**
	 * Default cache capacity (number of entries).
	 */
	protected final int DEFAULT_MAX_ENTRIES = 100;

	/**
	 * Max number of element in cache when considered unlimited.
	 */
	protected final int UNLIMITED = 2147483646;

	/**
	 * Cache capacity (number of entries).
	 */
	protected volatile int maxEntries = DEFAULT_MAX_ENTRIES;
	/**
	 * The table is rehashed when its size exceeds this threshold. (The value of this
	 * field is always (int)(capacity * loadFactor).)
	 *
	 * @serial
	 */
	protected int threshold;

	/**
	 * to inspect the cache key expire
	 */
	protected ConcurrentMap<K, Long> keyExpireTimeMap = new ConcurrentHashMap<>();
	protected ConcurrentMap<K, Long> keyIdleTimeOutMap = new ConcurrentHashMap<>();

	// 所有的缓存公用一个并行队列执行器
	public final static IParallelQueueExecutor DEFAULT_CACHE_QUEUE_EXECUTOR = new DisruptorParallelQueueExecutor(
			Runtime.getRuntime().availableProcessors() * 2, 2 << 12);
	// ParallelQueueExecutorBuilder.getInstance().builderSuperFastParallelQueueExecutor(4,
	// "cache-event-loop-default");

	private AtomicInteger epollCache = new AtomicInteger(0);
	private AtomicBoolean isClearCache = new AtomicBoolean(false);
	private AtomicBoolean isStartupCheckCapacity = new AtomicBoolean(false);
	private IParallelQueueExecutor iParallelQueueExecutor = null;
	private volatile String cacheTopic;

	public AbstractConcurrentCache(String cacheTopic,
			IParallelQueueExecutor iParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity,
			float loadFactor) {
		super(initialCapacity, loadFactor);
		this.maxEntries = initialCapacity;
		this.iExpireKeyAdaptor = iExpireKeyAdaptor;
		this.iParallelQueueExecutor = iParallelQueueExecutor;
		this.cacheTopic = cacheTopic;
		this.cacheManager = new MCacheManager<>(this, iParallelQueueExecutor, true);
	}

	public AbstractConcurrentCache(String cacheTopic,
			IParallelQueueExecutor iParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
		this(cacheTopic, iParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity,
				DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs a new, empty map with a default initial capacity and load factor.
	 */
	public AbstractConcurrentCache(String cacheTopic,
			IParallelQueueExecutor iParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		this(cacheTopic, iParallelQueueExecutor, iExpireKeyAdaptor,
				DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public AbstractConcurrentCache(String cacheTopic,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		this(cacheTopic, DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor,
				DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public AbstractConcurrentCache(String cacheTopic,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
		this(cacheTopic, DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor, initialCapacity,
				DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings.
	 */
	public synchronized boolean isEmpty() {
		return super.size() == 0;
	}

	/**
	 * Set the cache capacity
	 */
	public void setMaxEntries(int newLimit) {
		if (newLimit > 0) {
			maxEntries = newLimit;
		}
		else {
			// Capacity must be at least 1
			throw new IllegalArgumentException(
					"Cache maximum number of entries must be at least 1");
		}
	}

	/**
	 * Retrieve the cache capacity (number of entries).
	 */
	public int getMaxEntries() {
		return maxEntries;
	}

	/**
	 * Sets the memory caching flag.
	 */
	public void setMemoryCaching(boolean memoryCaching) {
		this.memoryCaching = memoryCaching;
	}

	/**
	 * Check if memory caching is used.
	 */
	public boolean isMemoryCaching() {
		return memoryCaching;
	}

	/**
	 * Sets the unlimited disk caching flag.
	 */
	public void setUnlimitedDiskCache(boolean unlimitedDiskCache) {
		this.unlimitedDiskCache = unlimitedDiskCache;
	}

	/**
	 * Check if we use unlimited disk cache.
	 */
	public boolean isUnlimitedDiskCache() {
		return unlimitedDiskCache;
	}

	/**
	 * Removes all mappings from this map.
	 */
	public void clear() {
		isClearCache.set(true);
		cacheManager.notifyListeners(
				new ObjectEvent<>(this, "", MCacheManager.clearCacheEvent));
	}

	@SuppressWarnings("unchecked")
	public V get(Object key) {
		if (log.isDebugEnabled()) {
			log.debug("get called (key=" + key + ")");
		}
		// 一级缓存取
		V e = super.get(key);
		if (e == null) {// 一级缓存取，如果一缓存没有，则到二级缓存取

			return e;
		}
		else {
			// checking for pointer equality first wins in most applications
			notifyCacheRateUpdate((K) key, ITEM_RETRIVE_OP);
			return e; //
		}

	}

	@Override
	public V get(K key, IMissCacheHandler<K, V> missCacheHandler) {

		if (log.isDebugEnabled()) {
			log.debug("get called (key=" + key + ")");
		}
		// 一级缓存取
		V e = super.get(key);
		if (e == null) {// 一级缓存取，如果一缓存没有，则到二级缓存取
			if (missCacheHandler != null) {
				e = missCacheHandler.missCacheHandler(key);
			}
			return e;
		}
		else {
			// checking for pointer equality first wins in most applications
			notifyCacheRateUpdate(key, ITEM_RETRIVE_OP);
			return e; //
		}
	}

	/**
	 * 这个 api 会自动将二级缓存中的数据存入一级缓存
	 */
	@Override
	public V getWithMissCacheHandler(K key) throws NoMissCacheHandlerException {

		if (log.isDebugEnabled()) {
			log.debug("get called (key=" + key + ")");
		}
		// 一级缓存取
		V e = super.get(key);
		if (e == null) {// 一级缓存取，如果一缓存没有，则到二级缓存取
			if (missCacheHandler == null) {
				throw new NoMissCacheHandlerException("the key[" + key
						+ "] does not IMissCacheHandler to operator,please implemate the interface for IMissCacheHandler.");
			}

			e = missCacheHandler.missCacheHandler(key);
			// 把他放到一级缓存
			this.put(key, e);
			return e;
		}
		else {
			// checking for pointer equality first wins in most applications
			notifyCacheRateUpdate(key, ITEM_RETRIVE_OP);
			return e; //
		}
	}

	@Override
	public synchronized V putIfAbsent(K key, V value) {
		if (super.containsKey(key)) {
			// 这里访问 会更改他的缓存因子
			return this.get(key);
		}

		return this.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	public class UpdateCacheRateEvent extends ObjectEvent<Object> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public K key;
		public int operatorType;

		public UpdateCacheRateEvent(Object source, Object value, int eventType) {
			super(source, value, eventType);
		}
	}

	public V put(K key, V value) {
		if (value instanceof Collection) {
			throw new UnsupportedOperationException(
					"the cache in memory can not in collection ");
		}
		/** OpenSymphony END */
		if (value == null) {
			throw new NullPointerException();
		}

		V oldValue = super.put(key, value);
		if (this.size() - this.maxEntries > 10) {
			// 加速回收过期的 cache entry
			cacheManager.handlerExpireCacheEntry(this);
		}
		// 1、这也是同步处理
		notifyCacheRateUpdate(key, ITEM_PUT_OP);
		return oldValue;
	}

	@Override
	public V remove(Object key) {
		keyExpireTimeMap.remove(key);
		keyIdleTimeOutMap.remove(key);
		return remove(key, true);
	}

	@SuppressWarnings("unchecked")
	public V remove(Object key, boolean isnotify) {
		V value = super.remove(key);
		if (isnotify) {
			notifyCacheRateUpdate((K) key, ITEM_REMOVE_OP);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object key, Object value) {
		if (key == null || value == null) {
			return false;
		}
		// 1、先从内存中移除
		boolean e = super.remove(key, value);
		if (e) {
			// 如果移除成功，则将size 数减少，并移除
			K k = (K) key;
			notifyCacheRateUpdate(k, ITEM_REMOVE_OP);
		}
		return e;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		boolean flag = super.replace(key, oldValue, newValue);
		if (flag) {
			// replace 成功才算是一次真正的访问
			notifyCacheRateUpdate(key, ITEM_RETRIVE_OP);
		}
		return flag;
	}

	@Override
	public V replace(K key, V value) {
		// 这里是强制行的replace
		V oldValue = super.replace(key, value);
		notifyCacheRateUpdate(key, ITEM_RETRIVE_OP);
		return oldValue;
	}

	/**
	 * Notify the underlying implementation that an item was put in the cache.
	 *
	 * @param key The cache key of the item that was put.
	 */
	public abstract void itemPut(K key);

	/**
	 * Notify any underlying algorithm that an item has been retrieved from the cache.
	 *
	 * @param key The cache key of the item that was retrieved.
	 */
	public abstract void itemRetrieved(K key);

	/**
	 * Notify the underlying implementation that an item was removed from the cache.
	 *
	 * @param key The cache key of the item that was removed.
	 */
	public abstract void itemRemoved(K key);

	/**
	 * The cache has reached its cacpacity and an item needs to be removed. (typically
	 * according to an algorithm such as LRU or FIFO).
	 *
	 * @return The key of whichever item was removed.
	 */
	public abstract K removeItem(boolean isRemove);

	@Override
	public Set<java.util.Map.Entry<K, V>> getAllEntrySet() {
		return super.entrySet();
	}

	@Override
	public Set<K> getAllKeySet() {
		return super.keySet();
	}

	private void notifyCacheRateUpdate(final K key, final int operateType) {
		epollCache.incrementAndGet();
		UpdateCacheRateEvent updateCacheRateEvent = new UpdateCacheRateEvent(this, "",
				MCacheManager.cacheRateUpdateEvent);
		updateCacheRateEvent.key = key;
		updateCacheRateEvent.operatorType |= operateType;
		cacheManager.updateCacheRate(updateCacheRateEvent);

		if (isStartupCheckCapacity.compareAndSet(false, true)) {
			// 每个 topic cache 都有自己的一个 capacity check event
			cacheManager.notifyListeners(
					new ObjectEvent<>(this, "", MCacheManager.checkCapacityEvent));
		}
	}

	public String getCacheTopic() {
		return cacheTopic;
	}

	public void setCacheTopic(String cacheTopic) {
		this.cacheTopic = cacheTopic;
	}

	public MCacheManager<K, V> getCacheEventLoop() {
		return cacheManager;
	}

	public void setCacheEventLoop(MCacheManager<K, V> cacheEventLoop) {
		this.cacheManager = cacheEventLoop;
	}

	public IExpireKeyHandler<K, V> getiExpireKeyAdaptor() {
		return iExpireKeyAdaptor;
	}

	public void setiExpireKeyAdaptor(IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		this.iExpireKeyAdaptor = iExpireKeyAdaptor;
	}

	public int getEpollCache() {
		return epollCache.get();
	}

	public void decrementAndGetEpollCache() {
		this.epollCache.decrementAndGet();
	}

	public boolean getIsClearCache() {
		return isClearCache.get();
	}

	public void setClearFinish() {
		isClearCache.set(false);
	}

	public boolean getIsStartupCheckCapacity() {
		return isStartupCheckCapacity.get();
	}

	public IParallelQueueExecutor getiParallelQueueExecutor() {
		return iParallelQueueExecutor;
	}

	public void setiParallelQueueExecutor(IParallelQueueExecutor iParallelQueueExecutor) {
		this.iParallelQueueExecutor = iParallelQueueExecutor;
	}

	public void setMissCacheHandler(IMissCacheHandler<K, V> missCacheHandler) {
		this.missCacheHandler = missCacheHandler;
	}

	/**
	 *
	 * @param key
	 * @param value
	 * @param expireTime 过期时间，以毫秒为单位
	 * @return
	 */
	@Override
	public V put(K key, V value, long expireTime) {
		keyExpireTimeMap.put(key, System.currentTimeMillis() + expireTime);
		keyIdleTimeOutMap.put(key, expireTime);
		return this.put(key, value);
	}

	@Override
	public void updateExpireTime(K key, long expireTime) {
		keyExpireTimeMap.put(key, expireTime);
	}

	public ConcurrentMap<K, Long> getKeyExpireTimeMap() {
		return new ConcurrentHashMap<>(keyExpireTimeMap);
	}

	@Override
	public long getIdleTimeOut(K key) {
		Long idleTimeOut = keyIdleTimeOutMap.get(key);

		return idleTimeOut == null ? -1 : idleTimeOut;
	}
}
