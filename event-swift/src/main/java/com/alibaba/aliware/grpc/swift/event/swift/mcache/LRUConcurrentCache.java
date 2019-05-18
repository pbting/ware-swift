package com.alibaba.aliware.grpc.swift.event.swift.mcache;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;

/**
 * LRU（long-recently） 最近最久未使用，是根据上一次访问时间到目前为止最长的时间进行替换，在这段时间内不考虑他的使用频率，是考虑他的
 * 访问时间
 * 
 * @author pbting
 *
 */
public class LRUConcurrentCache<K, V> extends AbstractConcurrentCache<K, V> {

	private static final long serialVersionUID = -7379608101794788534L;

	private LinkedBlockingQueue<K> list = new LinkedBlockingQueue<K>();

	public LRUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity, float loadFactor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
	}

	public LRUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity);
	}

	public LRUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor);
	}

	public LRUConcurrentCache(String cacheTopic,IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic,DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor);
	}
	
	// 这是为改变key的顺序而为子类留下的接口
	public void itemRetrieved(K key) {
		// 原子操作需同步
		list.remove(key);
		list.add(key);
	}

	/**
	 * 往里面放一个元素时，不管之前有没有，都remove掉，然后插入到
	 */
	public void itemPut(K key) {
		list.remove(key);
		list.add(key);
	}

	public K removeItem(boolean isRemove) {
		K toRemove = null;
		Iterator<K> it = list.iterator();
		toRemove = (K) it.next();
		if (isRemove) {
			it.remove();//
		}
		return toRemove;
	}

	public void itemRemoved(K key) {

		list.remove(key);
	}
}
