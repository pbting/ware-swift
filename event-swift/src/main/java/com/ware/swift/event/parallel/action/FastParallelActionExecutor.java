package com.ware.swift.event.parallel.action;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.ware.swift.event.parallel.DefaultThreadFactory;
import com.ware.swift.event.parallel.AbstractParallelQueueExecutor;

/**
 * 此设计模拟高速公路上车道设计。 n-1 条车道为正常处理车道，1条车道为应急车道。 并行队列 执行器 这里弥补了ParallelQueueExecutor
 * 的缺点：可以让同一种类型的车子在指定车道上跑。（也即同一种类型的 action 会被指定的 thread 执行）。消除频繁的上下问切换。 因此叫
 * FastParallelQueueExecutor。因此是吞吐量优先，所以相对于 SuperFastParallelQueueExecutor 来叫还是慢了半拍的
 * 
 * 【注意】适用于 有状态的Action instance
 * @author pengbingting
 *
 */
public class FastParallelActionExecutor extends AbstractParallelActionExecutor {

	private ThreadPoolExecutor[] pools;
	// 维护一个 可以水平缩放的并行队列。并行队列的大小 n = paralleleQueue.size() + 1
	// 其中paralleleQueue.size()为 n-1 条正常处理的车道，
	private ConcurrentMap<String, IActionQueue<Action>> paralleleQueue = new ConcurrentHashMap<>();
	private ReentrantLock lock = new ReentrantLock();
	private AbstractParallelQueueExecutor.ThreadPoolExecutorIndexAllocator indexAllocator;
	private AtomicBoolean isOperating = new AtomicBoolean(false);
	private AtomicBoolean isCronTrriger = new AtomicBoolean(false);

	/**
	 * 执行action队列的线程池
	 * 
	 * @param corePoolSize 最小线程数，包括空闲线程
	 * @param maxPoolSize 最大线程数
	 * @param keepAliveTime 当线程数大于核心时，终止多余的空闲线程等待新任务的最长时间
	 * @param cacheSize 执行队列大小
	 * @param prefix 线程池前缀名称
	 */
	public FastParallelActionExecutor(int threads, String prefix) {
		if (threads <= 0) {
			threads = AbstractParallelQueueExecutor.DEFAULT_QUEUE_SIZE;
		}

		pools = new ThreadPoolExecutor[threads];
		for (int i = 0; i < threads; i++) {
			// 半个小时之后还没有任务过来，则销毁线程
			pools[i] = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.MINUTES,
					new LinkedBlockingQueue<Runnable>(),
					new DefaultThreadFactory(prefix));
		}

		if (isPowerOfTwo(threads)) {
			indexAllocator = new AbstractParallelQueueExecutor.PowerOfTwoExecutorIndexChooser(threads);
		}
		else {
			indexAllocator = new AbstractParallelQueueExecutor.GenericExecutorIndexChooser(threads);
		}
	}

	/**
	 * 将一个 action 进入一个并行队列。可以水平缩放
	 * @param queueTopic
	 * @param action
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void enParallelAction(String queueTopic, Action action) {
		IActionQueue actionQueue = paralleleQueue.get(queueTopic);
		if (actionQueue == null) {
			try {
				lock.tryLock(3, TimeUnit.SECONDS);
				isOperating.set(true);
				actionQueue = paralleleQueue.get(queueTopic);
				if (actionQueue == null) {
					actionQueue = new FastParallelActionQueue(
							pools[indexAllocator.allocator()]);
					paralleleQueue.put(queueTopic, actionQueue);
				}
			}
			catch (Exception e) {
				actionQueue = new FastParallelActionQueue(
						pools[indexAllocator.allocator()]);
			}
			finally {
				isOperating.set(false);
				lock.unlock();
			}
		}
		action.setTopicName(queueTopic);
		trrigerWithRejectActionPolicy(actionQueue, action);
	}

	public void removeParallelAction(String queueTopic) {

		paralleleQueue.remove(queueTopic);
	}

	public void stop() {
		super.stop();
		for (int i = 0; i < pools.length; i++) {
			pools[i].shutdown();
		}
		paralleleQueue.clear();
	}

	@Override
	public void execute(Runnable command) {
		execute(command.getClass().getName(), command);
	}

	/**
	 * 如果这里不包装成一个Action，则跟SuperFastParallelQueueExecutor 没什么多大的区别
	 */
	@Override
	public void execute(String topic, Runnable command) {
		if (!(command instanceof Action)) {
			ActionBridge actionBridge = new ActionBridge(command);
			enParallelAction(topic, actionBridge);
		}
		else {
			Action action = (Action) command;
			enParallelAction(topic, action);
		}
	}

	@Override
	public void adjustPoolSize(int newCorePoolSize, int newMaxiPoolSize) {
		// nothing to do
	}

	@Override
	public void registerTopics(String... topics) {
		if (topics == null || topics.length == 0) {
			return;
		}

		lock.lock();
		try {
			for (String queueTopic : topics) {
				IActionQueue<Action> actionQueue = paralleleQueue.get(queueTopic);
				if (actionQueue != null) {
					continue;
				}

				actionQueue = new FastParallelActionQueue(
						pools[indexAllocator.allocator()]);
				paralleleQueue.put(queueTopic, actionQueue);
			}
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void removeTopic(String topic) {

		paralleleQueue.remove(topic);
	}

	@Override
	public void executeOneTime(Runnable command) {
		if (command instanceof Action) {
			Action action = (Action) command;
			executeOneTimeAction(action);
		}
		else {
			ActionBridge actionBridge = new ActionBridge(command);
			executeOneTimeAction(actionBridge);
		}
	}

	@Override
	public void executeOneTimeAction(Action action) {
		IActionQueue<Action> fastActionQueue = new FastParallelActionQueue(
				pools[indexAllocator.allocator()]);

		fastActionQueue.enqueue(action);
	}

}
