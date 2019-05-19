package com.ware.swift.event.mcache;

import com.ware.swift.event.common.Log;
import com.ware.swift.event.parallel.IParallelQueueExecutor;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 最少使用缓存置换算法的实现latest-seldom use cache
 * @author pbting
 *
 */
public class LFUConcurrentCache<K,V> extends AbstractConcurrentCache<K,V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LFUConcurrentCache(String cacheTopic, IParallelQueueExecutor IParallelQueueExecutor, IExpireKeyHandler<K, V> iExpireKeyAdaptor,
							  int initialCapacity, float loadFactor) {
		super(cacheTopic,IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity, loadFactor);
	}

	public LFUConcurrentCache(String cacheTopic,IParallelQueueExecutor IParallelQueueExecutor, IExpireKeyHandler<K, V> iExpireKeyAdaptor,
			int initialCapacity) {
		super(cacheTopic,IParallelQueueExecutor, iExpireKeyAdaptor, initialCapacity);
	}

	public LFUConcurrentCache(String cacheTopic,IParallelQueueExecutor IParallelQueueExecutor,
			IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic,IParallelQueueExecutor, iExpireKeyAdaptor);
	}


	public LFUConcurrentCache(String cacheTopic,IExpireKeyHandler<K, V> iExpireKeyAdaptor) {
		super(cacheTopic,DEFAULT_CACHE_QUEUE_EXECUTOR, iExpireKeyAdaptor);
	}
	
	private static final Logger log = LoggerFactory.getLogger(Log.class);
	
	//定义用来存放访问次数Set集合
	private Set<Integer> accessCountSort = new ConcurrentSkipListSet<Integer>(new Comparator<Integer>() {
		public int compare(Integer o1, Integer o2) {
			
			return o1.compareTo(o2);
		};
	});
	
	/**
	 * key和访问次数据映射的hashmap
	 */
	private ConcurrentHashMap<K, Integer> keyAccessCount = new ConcurrentHashMap<K, Integer>();
	
	/**
	 * 访问映射字段：次数和这一类key的映射排序分类处理
	 */
	private ConcurrentHashMap<Integer, LinkedBlockingQueue<K>> accessCountQueue = new ConcurrentHashMap<Integer, LinkedBlockingQueue<K>>();
	
	@Override
	public void itemPut(K key) {
		
		if(keyAccessCount.containsKey(key)){//已经包含该key，则该怎么办
			/**
			 * 先把这个key取出来，然后增加次数,放到适当的等级中
			 *这个时候就成了一个获取时的操作了，改变其次序 
			 */
			log.info("[HighCache]-LFUConcurrentCache.itemPut and containsKey:"+key);
			this.itemRetrieved(key);
		}else{//不包含该怎么办
			//得到一次级别上的所有map对象，放到第一个级别上
			LinkedBlockingQueue<K> accessCountQueue =  this.accessCountQueue.get(1);
			if(accessCountQueue == null){
				//表示系统的第一次
				accessCountQueue = new LinkedBlockingQueue<K>();
				this.accessCountQueue.put(1, accessCountQueue);
			}
			
			accessCountQueue.add(key);
			if(!this.accessCountSort.contains(1)){
				this.accessCountSort.add(1);
			}
			
			this.keyAccessCount.put(key,1);
			log.debug("[HighCache]-LFUConcurrentCache.itemPut and first:"+key);
			//一次处理处理完成
		}
	}

	/**
	 * 当从缓存容器中get操作，促发该动作，目的就是改变key的一个排序
	 */
	@Override
	public void itemRetrieved(K key) {
		//1、获取当前key的一个访问次数，
		if(!keyAccessCount.containsKey(key)){
			return ;
		}
		
		Integer accessCount = keyAccessCount.get(key);
		//获取这个访问次次数的一批key
		LinkedBlockingQueue<K> accessCountQueue =  this.accessCountQueue.get(accessCount);
		//从当前这个等级remove 掉，往下一个等级放
		accessCountQueue.remove(key);
		
		if(accessCountQueue.isEmpty()){
			this.accessCountQueue.remove(accessCount);
			accessCountQueue.clear();
			accessCountQueue = null ;
			//不需要在排序列表里面
			accessCountSort.remove(accessCount);
		}
		//对这访问次数加一
		accessCount++;
		log.debug("[HighCache]-LFUConcurrentCache.itemRetrieved and the access count is"+accessCount);
		//获得下一个等级
		LinkedBlockingQueue<K> nextLevelAccessCountQueue =  this.accessCountQueue.get(accessCount);
		
		if(nextLevelAccessCountQueue == null){
			nextLevelAccessCountQueue = new LinkedBlockingQueue<K>();
			this.accessCountQueue.put(accessCount, nextLevelAccessCountQueue);
		}
		//然后在这一个等级内添加key和他访问次数的一个映射
		nextLevelAccessCountQueue.add(key);
		//更新当前 key的访问次数
		this.keyAccessCount.put(key, accessCount);
		//必须的放进去
		log.debug("访问频率最高的次数为："+accessCount+",and the key is:"+key);
		if(!this.accessCountSort.contains(accessCount)){
			accessCountSort.add(accessCount);
		}
		//一次处理完成
	}

	/**
	 * 缓存容器中remove时，促发该动作，移除该key，重新保持最新的次序
	 */
	@Override
	public void itemRemoved(K key) {
		
		if(!this.keyAccessCount.containsKey(key))
			return ;
			
		Integer count = keyAccessCount.remove(key);//移除这个key所对应的访问次数
		//获取这个访问次次数的一批key
		LinkedBlockingQueue<K> accessCountQueue =  this.accessCountQueue.get(count);
		accessCountQueue.remove(key);
		log.debug("the remove key is: "+key+" and the access count is: "+count);
	}

	/**
	 * 一级缓存和二级缓存兑换时，自动替换一个缓存实体,这里是核心
	 * 
	 * 有个一次级别全部扫描的关系在里面，对相同的次数又该如何处理
	 * ,这里一定要确保移除掉，不要就会影响命中率
	 * 
	 */
	@Override
	public K removeItem(boolean isRemove) {
		K key = null ;
		//从低级别的开始扫描,也即排序迭代
		for(Integer count : this.accessCountSort){
			LinkedBlockingQueue<K> accessCountQueue =  this.accessCountQueue.get(count);
			if(accessCountQueue.isEmpty())//这个时候表明这个访问层已经清空，则跳到下一层
				continue;
			//移除这一类级别中的任何一个数，因为这些的访问次数是相同的
			else{
				//并将在HashMap中的已移除的
				if(isRemove){
					key = accessCountQueue.poll();
					keyAccessCount.remove(key);
				}else{
					key = accessCountQueue.peek();
				}
				break;
			}
		}
		//如果正常，则应该替换村次数最少的
		
		return key;
	}
}
