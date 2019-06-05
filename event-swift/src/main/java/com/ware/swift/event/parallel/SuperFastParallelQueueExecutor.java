package com.ware.swift.event.parallel;

import com.ware.swift.event.common.AtomicLongMap;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 此设计模拟高速公路上车道设计。 n-1 条车道为正常处理车道，1条车道为应急车道。 并行队列 执行器
 * <p>
 * 叫 SuperFastParallelQueueExecutor。是因为响应式优先。没有两层的并行队列，只有一层的并行执行队列
 */
public class SuperFastParallelQueueExecutor extends AbstractParallelQueueExecutor {

    private ThreadPoolExecutor[] pools;
    // 维护一个 可以水平缩放的并行队列。并行队列的大小 n = parallel Queue.size() + 1
    private ReentrantLock lock = new ReentrantLock(true);
    private ThreadPoolExecutorIndexAllocator threadPoolExecutorIndexChooser;
    private ConcurrentMap<String, Integer> queueExecutorMapping = new ConcurrentHashMap<>();
    private AtomicLongMap<String> topicLastExecuteTime = AtomicLongMap.create();
    private AtomicBoolean isOperating = new AtomicBoolean(false);
    private AtomicBoolean isCronTrriger = new AtomicBoolean(false);

    /**
     * @param prefix 线程enen池前缀名称
     */
    public SuperFastParallelQueueExecutor(int threads, String prefix) {
        if (threads <= 0) {
            threads = DEFAULT_QUEUE_SIZE;
        }

        pools = new ThreadPoolExecutor[threads];
        for (int i = 0; i < threads; i++) {
            // 半个小时之后还没有任务过来，则销毁线程
            pools[i] = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>(),
                    new DefaultThreadFactory(prefix));
        }

        if (isPowerOfTwo(threads)) {
            threadPoolExecutorIndexChooser = new PowerOfTwoExecutorIndexChooser(
                    pools.length);
        } else {
            threadPoolExecutorIndexChooser = new GenericExecutorIndexChooser(
                    pools.length);
        }
    }

    @Override
    public void execute(String queueTopic, Runnable command) {
        Integer index = queueExecutorMapping.get(queueTopic);
        if (index == null) {
            try {
                lock.tryLock(3, TimeUnit.SECONDS);
                isOperating.set(true);
                index = queueExecutorMapping.get(queueTopic);
                if (index == null) {
                    index = threadPoolExecutorIndexChooser.allocator();
                    queueExecutorMapping.put(queueTopic, index);
                }
            } catch (Exception e) {
                if (index == null) {
                    index = threadPoolExecutorIndexChooser.allocator();
                }
            } finally {
                isOperating.set(false);
                lock.unlock();
            }
        }
        ThreadPoolExecutor executor = pools[index];
        runing(executor, command);
        topicLastExecuteTime.put(queueTopic, System.currentTimeMillis());
    }

    public void stop() {
        super.stop();
        for (int i = 0; i < pools.length; i++) {
            pools[i].shutdown();
        }
    }

    private void runing(ThreadPoolExecutor executor, Runnable task) {
        if (executor.getQueue().isEmpty()) {
            executor.execute(task);
        } else {
            executor.getQueue().offer(task);// (action);
        }
    }

    public void execute(Runnable command) {
        execute(command.getClass().getName(), command);
    }

    @Override
    public void registerTopics(String... topics) {
        if (topics == null || topics.length == 0) {
            return;
        }

        lock.lock();
        try {
            for (String queueTopic : topics) {
                Integer index = queueExecutorMapping.get(queueTopic);
                if (index != null) {
                    continue;
                }

                index = threadPoolExecutorIndexChooser.allocator();
                queueExecutorMapping.put(queueTopic, index);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeTopic(String topic) {

        queueExecutorMapping.remove(topic);
    }

    @Override
    public void executeOneTime(Runnable command) {
        ThreadPoolExecutor executor = pools[threadPoolExecutorIndexChooser.allocator()];
        runing(executor, command);
    }
}
