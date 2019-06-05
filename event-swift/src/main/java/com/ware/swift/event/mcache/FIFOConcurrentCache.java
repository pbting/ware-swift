package com.ware.swift.event.mcache;

import com.ware.swift.event.parallel.IParallelQueueExecutor;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * 只关心key 的 put 操作。当元素满的时候将最先进入的元素给 remove 掉
 * 注意：get/replace 等操作是不会更改key 的进入先后顺序的
 *
 * @param <K>
 * @param <V>
 * @author pengbingting
 */
public class FIFOConcurrentCache<K, V> extends AbstractConcurrentCache<K, V> {

    public FIFOConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor, IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
        super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor);
    }

    public FIFOConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor, int initialCapacity) {
        super(cacheTopic, IParallelQueueExecutor, new DefaultExpireKeyAdaptor<>(), initialCapacity);
    }

    public FIFOConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor, IExpireKeyHandler<K, V> iExpireKeyAdaptor,
                               int initialCapacity, float loadFactor) {
        super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
    }

    public FIFOConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor, IExpireKeyHandler<K, V> iExpireKeyAdaptor,
                               int initialCapacity) {
        super(cacheTopic, IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity);
    }

    public FIFOConcurrentCache(String cacheTopic, IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
        super(cacheTopic, DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor, initialCapacity);
    }

    private static final long serialVersionUID = -10333778645392679L;

    private LinkedBlockingQueue<K> linkedQuence = new LinkedBlockingQueue<>();

    public void itemPut(K key) {
        if (linkedQuence.contains(key)) {
            linkedQuence.remove(key);
        }

        linkedQuence.add(key);
    }

    @Override
    public void itemRetrieved(K key) {
        //get operation does not update the order
    }

    @Override
    public void itemRemoved(K key) {
        linkedQuence.remove(key);
    }

    /**
     * 仅仅是提供找到合适 key 的算法，但是不做真正的remove
     */
    @Override
    public K removeItem(boolean isRemove) {
        if (isRemove) {
            return linkedQuence.poll();
        } else {
            return linkedQuence.peek();
        }
    }

}
