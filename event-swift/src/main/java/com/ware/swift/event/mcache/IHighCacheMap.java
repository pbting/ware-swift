package com.ware.swift.event.mcache;

import java.util.Set;

interface IHighCacheMap<K, V> {

	/**
	 * The default initial number of table slots for this table (32). Used when not
	 * otherwise specified in constructor.
	 **/
	int DEFAULT_INITIAL_CAPACITY = 32;

	/**
	 * The minimum capacity. Used if a lower value is implicitly specified by either of
	 * the constructors with arguments. MUST be a power of two.
	 */
	int MINIMUM_CAPACITY = 4;

	/**
	 * The maximum capacity. Used if a higher value is implicitly specified by either of
	 * the constructors with arguments. MUST be a power of two <= 1<<30.
	 */
	int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The default load factor for this table. Used when not otherwise specified in
	 * constructor, the default is 0.75f.
	 **/
	float DEFAULT_LOAD_FACTOR = 0.75f;

	// 设置最大的缓存实体
	void setMaxEntries(int newLimit);

	int getMaxEntries();

	// 是指是否支持内存缓存
	void setMemoryCaching(boolean memoryCaching);

	// 设置是否支持二级缓存
	void setUnlimitedDiskCache(boolean unlimitedDiskCache);

	// 清除缓存
	void clear();

	// 从缓存中得到一个缓存数据项
	V get(K key);

	// 根据key获取指定的value 时，如果缓存未命中，可以根据指定的 缓存未命中handler 来处，从其他地方（redis,mysql and so on）获取值
	V get(K key, IMissCacheHandler<K, V> missCacheHandler);

	// 如果在一开始设置了缓存未命中时的handler,则调用此api，会在缓存未命中时触发相关的miss cache handler
	V getWithMissCacheHandler(K key) throws NoMissCacheHandlerException;

	// 像缓存容器中放入一个缓存数据项
	V put(K key, V value);

	/**
	 *
	 * @param key
	 * @param value
	 * @param expireTime 过期时间，以毫秒为单位
	 * @return
	 */
	V put(K key, V value, long expireTime);

	/**
	 *
	 * @param key
	 * @param expireTime
	 */
	void updateExpireTime(K key, long expireTime);

	/**
	 *
	 * @param key
	 * @return
	 */
	long getIdleTimeOut(K key);

	// 向缓存容器中移除一个缓存数据项
	V remove(Object key);

	// 得到当前缓存容器的一个大小
	int size();

	/**
	 * 
	 * @param key
	 */
	void itemPut(K key);

	/**
	 * Notify any underlying algorithm that an item has been retrieved from the cache.
	 *
	 * @param key The cache key of the item that was retrieved.
	 */
	void itemRetrieved(K key);

	/**
	 * Notify the underlying implementation that an item was removed from the cache.
	 *
	 * @param key The cache key of the item that was removed.
	 */
	void itemRemoved(K key);

	/**
	 * The cache has reached its cacpacity and an item needs to be removed. (typically
	 * according to an algorithm such as LRU or FIFO).
	 *
	 * @return The key of whichever item was removed.
	 */
	K removeItem(boolean isRemove);

	Set<java.util.Map.Entry<K, V>> getAllEntrySet();

	Set<K> getAllKeySet();
}
