package com.alibaba.aliware.grpc.swift.event.swift;

import com.alibaba.aliware.grpc.swift.event.swift.object.IEventCallBack;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.IParallelQueueExecutor;

/**
 * 
 * @param <E>
 */
public interface IAsyncEventObject<E> {

	/**
	 * 
	 */
	void shutdown();

	/**
	 * 调整线程池的大小
	 */
	void adjustExecutor(int coreSize, int maxSize);

	/**
	 * 可以进应急队列
	 */
	void enEmergencyQueue(Runnable runnable);

	/**
	 * 在一个partition key中 的 object event 是保证有序执行的。如何选择 partition key 直接决定了
	 * 你程序中的并发性。一个好的Partition Key设计常常会大幅提高程序的运行性能。 首先，由于Partition Key用来控制这个event
	 * 将会进入哪个queue。因此在响应一个object event 时，较少在多个thread间切换能够提供较高的性能。
	 * 
	 */
	String partitioner(ObjectEvent<E> event);

	/**
	 * 
	 */
	IParallelQueueExecutor getParallelQueueExecutor();

	/**
	 * 提供事件回调的机制
	 */
	void publish(E value, Integer eventType, IEventCallBack iEventCallBack);

	/**
	 * 
	 * @param objectEvent
	 * @param iEventCallBack
	 */
	void notifyListeners(ObjectEvent<E> objectEvent, IEventCallBack iEventCallBack);

}
