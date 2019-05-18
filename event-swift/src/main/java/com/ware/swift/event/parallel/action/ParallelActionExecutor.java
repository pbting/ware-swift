package com.ware.swift.event.parallel.action;

import com.ware.swift.event.common.Log;

import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.ware.swift.event.parallel.DefaultThreadFactory;
import com.ware.swift.event.parallel.ThreadPoolContainer;

/**
 * 
 * 此设计模拟高速公路上车道设计。 n-1 条车道为正常处理车道，1条车道为应急车道。 并行队列 执行器
 * 
 * 这里的主要缺点就是：同一种类型的车子可以在任何条车道上跑。（也即同一种类型的 action 会被多个不同的 thread 执行）。存在频繁的在多个线程间切换。
 * 【注意】适合无状态的Action instance 因为可以动态扩展线程池数量，因此在突发流量的情况下可以适当的增大线程数量，提供并发处理的能力。
 * @author pengbingting
 */
public class ParallelActionExecutor extends AbstractParallelActionExecutor {

	private ThreadPoolExecutor pool;
	private String prefix;
	// 维护一个 可以水平缩放的并行队列。并行队列的大小 n = paralleleQueue.size() + 1
	// 其中paralleleQueue.size()为 n-1 条正常处理的车道，
	private ConcurrentMap<String, IActionQueue<Action>> paralleleQueue = new ConcurrentHashMap<String, IActionQueue<Action>>();
	private ReentrantLock lock = new ReentrantLock();
	private AtomicBoolean isOperating = new AtomicBoolean(false);
	private AtomicBoolean isCronTrriger = new AtomicBoolean(false);

	public ParallelActionExecutor(int corePoolSize, int maxPoolSize, int keepAliveTime,
			String prefix) {

		this(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES, prefix,
				new DiscardOldestPolicy());
	}

	public ParallelActionExecutor(int corePoolSize, int maxPoolSize, int keepAliveTime,
			TimeUnit unit, String prefix) {

		this(corePoolSize, maxPoolSize, keepAliveTime, unit, prefix,
				new DiscardOldestPolicy());
	}

	/**
	 * 执行action队列的线程池
	 * 
	 * @param corePoolSize 最小线程数，包括空闲线程
	 * @param maxPoolSize 最大线程数
	 * @param keepAliveTime 当线程数大于核心时，终止多余的空闲线程等待新任务的最长时间
	 * @param prefix 线程池前缀名称
	 * @param rejectedExecutionHandler 拒绝策略
	 */
	public ParallelActionExecutor(int corePoolSize, int maxPoolSize, int keepAliveTime,
			String prefix, RejectedExecutionHandler rejectedExecutionHandler) {

		this(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES, prefix,
				rejectedExecutionHandler);
	}

	public ParallelActionExecutor(int corePoolSize, int maxPoolSize, int keepAliveTime,
			TimeUnit unit, String prefix,
			RejectedExecutionHandler rejectedExecutionHandler) {
		LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(
				Integer.MAX_VALUE);// 无界队列，不需 处理策略
		if (prefix == null) {
			prefix = "";
		}
		this.prefix = prefix;
		ThreadFactory threadFactory = new DefaultThreadFactory(prefix);
		pool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit,
				workQueue, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 定制化配置
	 * @param pool
	 */
	public ParallelActionExecutor(ThreadPoolExecutor pool) {
		this.pool = pool;
	}

	public ThreadPoolContainer getThreadPollContainer() {

		ThreadPoolContainer threadPoolContainer = new ThreadPoolContainer();
		threadPoolContainer.setThreadSize(this.pool.getPoolSize());
		threadPoolContainer.setLargestPoolSize(
				this.pool.getLargestPoolSize() + this.pool.getPoolSize());
		threadPoolContainer.setActiveTaskCount(this.pool.getActiveCount());
		threadPoolContainer.setCompleteTaskCount((int) this.pool.getCompletedTaskCount());
		threadPoolContainer.setWaitTaskCount(this.pool.getQueue().size());
		threadPoolContainer.setTaskCount((int) this.pool.getTaskCount());
		return threadPoolContainer;
	}

	/**
	 * if you execute a temporary or disposable action then call this method,or normal and
	 * executor more times you should be call the enDefaultQueue method
	 * @param action
	 */
	public void execute(Runnable command) {
		String topic = command.getClass().getName();
		execute(topic, command);
	}

	@Override
	public void execute(String topic, Runnable command) {
		if (!(command instanceof Action)) {
			// 如果不是 Action 的实例，则桥接一个
			enParallelAction(topic, new ActionBridge(command));
		}
		else {
			Action tmpAction = (Action) command;
			enParallelAction(topic, tmpAction);
		}
	}

	/**
	 * @param newCorePoolSize
	 * @param newMaxiPoolSize
	 */
	public void adjustPoolSize(int newCorePoolSize, int newMaxiPoolSize) {
		if (newCorePoolSize != pool.getCorePoolSize()) {
			if (newCorePoolSize > 0 && newCorePoolSize < 357) {
				pool.setCorePoolSize(newCorePoolSize);
			}

			if (newMaxiPoolSize > 0 && newMaxiPoolSize < 357) {
				pool.setMaximumPoolSize(newCorePoolSize);
			}
		}
	}

	public boolean isShutdown() {

		return pool.isShutdown();
	}

	public String getPrefix() {

		return this.prefix;
	}

	public <T> T submit(Callable<T> command) {
		try {
			return pool.submit(command).get();
		}
		catch (InterruptedException e) {
			Log.error(this.getClass().getName() + "; submit a task excetion.", e);
			e.printStackTrace();
		}
		catch (ExecutionException e) {
			Log.error(this.getClass().getName() + "; submit a task excetion.", e);
			e.printStackTrace();
		}
		return null;
	}

	public void stop() {
		super.stop();
		if (!pool.isShutdown()) {
			pool.shutdown();
		}

		for (IActionQueue<Action> actionQueue : paralleleQueue.values()) {
			actionQueue.clear();
		}
	}

	/**
	 * 将一个 action 进入一个并行队列。可以水平缩放
	 * @param queueTopic
	 * @param action
	 */
	@Override
	public void enParallelAction(String queueTopic, Action action) {
		IActionQueue<Action> actionQueue = paralleleQueue.get(queueTopic);
		if (actionQueue == null) {
			try {
				lock.tryLock(3, TimeUnit.SECONDS);
				isOperating.set(true);
				actionQueue = paralleleQueue.get(queueTopic);
				if (actionQueue == null) {
					actionQueue = new ParallelActionQueue(pool);
					paralleleQueue.put(queueTopic, actionQueue);
				}
			}
			catch (Exception e) {
				// 给一个临时队列
				actionQueue = new ParallelActionQueue(pool);
			}
			finally {
				isOperating.set(false);
				lock.unlock();
			}
		}
		action.setTopicName(queueTopic);
		trrigerWithRejectActionPolicy(actionQueue, action);
	}

	@Override
	public void removeParallelAction(String queueTopic) {
		paralleleQueue.remove(queueTopic);
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

				actionQueue = new ParallelActionQueue(pool);
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
		IActionQueue<Action> actionQueue = new ParallelActionQueue(pool);
		actionQueue.enqueue(action);
	}
}
