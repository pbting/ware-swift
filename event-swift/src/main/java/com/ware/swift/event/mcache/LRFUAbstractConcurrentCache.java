package com.ware.swift.event.mcache;

import com.ware.swift.event.parallel.IParallelQueueExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 最少使用缓存置换算法的实现latest-seldom use cache
 *
 * @author pbting
 */
public abstract class LRFUAbstractConcurrentCache<K, V> extends AbstractConcurrentCache<K, V> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    // 定义用来存放访问次数Set集合
    private Set<Float> cacheRateSort = new ConcurrentSkipListSet<Float>(new Comparator<Float>() {
        public int compare(Float o1, Float o2) {
            // 从小到大排序
            return o1.compareTo(o2);
        }

        ;
    });

    /**
     * key和访问次数据映射的hashmap
     */
    private ConcurrentHashMap<K, Float> keyCacheRateMap = new ConcurrentHashMap<>();

    /**
     * 访问映射字段：次数和这一类key的映射排序分类处理
     */
    private ConcurrentHashMap<Float, Map<K, SRUKey>> cacheRateMapList = new ConcurrentHashMap<>();


    public LRFUAbstractConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelActionExecutor,
                                       IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity, float loadFactor) {
        super(cacheTopic, IParallelActionExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
    }

    public LRFUAbstractConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelActionExecutor,
                                       IExpireKeyHandler<K, V> iExpireKeyAdaptor, int initialCapacity) {
        super(cacheTopic, IParallelActionExecutor, iExpireKeyAdaptor, initialCapacity);
    }

    public LRFUAbstractConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelActionExecutor,
                                       IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
        super(cacheTopic, IParallelActionExecutor, iExpireKeyAdaptor);
    }

    public LRFUAbstractConcurrentCache(String cacheTopic, IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
        super(cacheTopic, DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor);
    }

    private static final Logger log = LoggerFactory.getLogger(LRFUAbstractConcurrentCache.class);

    // 给一个增强因子，默认为10
    protected final static int EFACTOR = 2 << 4;

    @Override
    public void itemPut(K key) {
        if (keyCacheRateMap.containsKey(key)) {// 已经包含该key，则该怎么办
            /**
             * 先把这个key取出来，然后增加次数,放到适当的等级中 这个时候就成了一个获取时的操作了，改变其次序
             */
            log.info("[HighCache]-LFUConcurrentCache.itemPut and containsKey:" + key);
            this.itemRetrieved(key);
        } else {// 不包含该怎么办
            SRUKey lfuKey = new SRUKey(key);
            lfuKey.incrementCount();// 对次数进行加一，同时还有一些任务需要处理，
            // 得到这个key的响应比
            float cacheRate = lfuKey.getCacheRate();
            // 得到一次级别上的所有map对象，放到第一个级别上
            Map<K, SRUKey> ES = this.cacheRateMapList.get(cacheRate);

            if (ES == null) {
                // 表示系统的第一次
                ES = new ConcurrentHashMap<>();
                this.cacheRateMapList.put(cacheRate, ES);
            }

            ES.put(key, lfuKey);
            // 如果为空，则表示第一次的，以后的第一次就可以不添加了,目的是排序
            if (!this.cacheRateSort.contains(cacheRate)) {
                this.cacheRateSort.add(cacheRate);
            }

            // 就算有一次也要记录下来
            this.keyCacheRateMap.put(key, cacheRate);
            log.debug("[HighCache]-LFUConcurrentCache.itemPut and first:" + key);
            // 一次处理处理完成
        }
    }

    /**
     * 当从缓存容器中get操作，促发该动作，目的就是改变key的一个排序
     */
    @Override
    public void itemRetrieved(K key) {
        // 如果这个key都没有响应的缓存响应因子，则直接返回
        if (!keyCacheRateMap.containsKey(key)) {
            return;
        }

        // 1、获取当前key的一个响应比，
        Float cacheRate = keyCacheRateMap.get(key);
        // 获取这个访问次次数的一批key
        Map<K, SRUKey> keyMap = this.cacheRateMapList.get(cacheRate);
        // 这个时候存在再次放入，则提高一次级别，的从原来的级别中移除
        SRUKey lFUKey = keyMap.remove(key);
        //防止内存持续增长，需要判断：如果当前这个 响应因子层级没有对应于其他的key了，则立即回收
        if (keyMap.isEmpty()) {
            this.cacheRateMapList.remove(cacheRate);
            keyMap.clear();
            //同时，也不需要在排序列表里面了
            cacheRateSort.remove(cacheRate);
        }

        // 对这访问次数加一，同时得到新的一个响应比
        lFUKey.incrementCount();
        cacheRate = lFUKey.getCacheRate();

        log.debug("[HighCache]-LFUConcurrentCache.itemRetrieved and the access count is" + cacheRate);
        // 根据新的响应比，获取该响应比相同的key-sets
        Map<K, SRUKey> nextLevelKeyMap = this.cacheRateMapList.get(cacheRate);

        if (nextLevelKeyMap == null) {
            nextLevelKeyMap = new ConcurrentHashMap<>();
            this.cacheRateMapList.put(cacheRate, nextLevelKeyMap);
        }
        // 然后在这一个等级内添加key和他访问次数的一个映射
        nextLevelKeyMap.put(key, lFUKey);
        //更新
        this.keyCacheRateMap.put(key, cacheRate);
        // 必须的放进去
        // 对这个访问次数进行排序,重复的不会被添加,表明出现访问次数最高的
        log.debug("访问频率最高的次数为：" + lFUKey.count + ",and the key is:" + lFUKey.key);

        if (!this.cacheRateSort.contains(cacheRate)) {
            cacheRateSort.add(cacheRate);
        }
        // 一次处理完成
    }

    /**
     * 缓存容器中remove时，促发该动作，移除该key，重新保持最新的次序
     */
    @Override
    public void itemRemoved(K key) {
        // 如果这个key不存在响应的缓存相应因子。则直接返回
        if (!keyCacheRateMap.containsKey(key)) {
            return;
        }

        Float cacheRate = keyCacheRateMap.remove(key);
        // 获取这个访问次次数的一批key
        Map<K, SRUKey> levelLFUKeys = this.cacheRateMapList.get(cacheRate);
        // 这个时候存在再次放入，则提高一次级别，的从原来的级别中移除
        SRUKey removeKey = levelLFUKeys.remove(key);
        //如果当前缓存因子没有对应的元素，则将在排序中的元素也 remove 掉
        if (levelLFUKeys.isEmpty()) {
            cacheRateSort.remove(cacheRate);
        }
        log.debug("the remove key is" + removeKey.key + " and the access count is:" + removeKey.currentCacheRate);
    }

    /**
     * 一级缓存和二级缓存兑换时，自动替换一个缓存实体,这里是核心 有个一次级别全部扫描的关系在里面，对相同的次数又该如何处理 ,
     * 注意：这里仅仅是找一个 remove 的key,真正remove 的操作，是通过 itemRemoved 方法来移除的
     */
    @Override
    public K removeItem(boolean isRemove) {
        // 要记录哪一组哪一个key，然后才可以更好的移除
        K removeKey = null;
        // 从低级别的开始扫描,也即排序迭代
        try {
            for (Float cacheRate : this.cacheRateSort) {
                Map<K, SRUKey> levelLFUKeys = this.cacheRateMapList.get(cacheRate);
                if (levelLFUKeys != null && levelLFUKeys.isEmpty()) {// 这个时候表明这个访问层已经清空，则跳到下一层
                    continue;
                    // 移除这一类级别中的任何一个数，因为这些的访问次数是相同的
                } else {
                    // 得到key的集合，移除响应比最小的
                    Iterator<K> iter = levelLFUKeys.keySet().iterator();
                    removeKey = iter.next();
                    if (isRemove) {
                        iter.remove();
                        keyCacheRateMap.remove(removeKey);
                    }
                    break;
                }
            }
        } catch (Exception e) {
        }
        // 返回真正的
        return removeKey;
    }

    protected volatile Long createTime = null;

    public class SRUKey {
        protected K key;
        protected int count = 0;
        protected long lastAccessTime = 0;
        long lastIntervalTime = 0;
        float lastFactor = 0.0f;
        float lastDx = 0.0f;
        Float currentCacheRate = 0F;

        // 记下这个key的创建时间
        public SRUKey(K key) {
            this.key = key;
            if (createTime == null) {
                createTime = System.nanoTime();
            }

            this.lastAccessTime = System.nanoTime();
        }

        // 每次设置这个count值时表示一次访问，则这个时候更改lastAccessTime 的时间
        public void incrementCount() {
            this.count++;
            // 记录遇上一次访问时间的一个间隔
            lastIntervalTime = System.nanoTime() - this.lastAccessTime + EFACTOR;
            this.lastAccessTime = System.nanoTime();
        }

        /**
         * 计算这个key的一个缓存响应比，如果这个缓存响应比越高，则越不应该留下，越小，则越应该留下
         */
        public Float getCacheRate() {
            // 根据方差值来计算缓存响应比
            float factor = getFactor(this);
            //注意区分这两种算法的实现
            if (LRFUAbstractConcurrentCache.this instanceof LRFUByDxConcurrentCache) {
                factor = (float) Math.sqrt(factor);
                factor = (float) Math.sqrt(factor);
                this.currentCacheRate = Math.abs(Float.valueOf(this.count * this.count) / factor);
            } else if (LRFUAbstractConcurrentCache.this instanceof LRFUByExConcurrentCache) {
                this.currentCacheRate = Math.abs((float) Math.sqrt(factor * this.count));
            }
            this.currentCacheRate = (float) Math.sqrt(currentCacheRate);
            this.currentCacheRate = (float) Math.sqrt(currentCacheRate);
            return this.currentCacheRate;
        }
    }

    public abstract float getFactor(SRUKey sruKey);
}
