package com.ware.swift.event.parallel.action;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于响应式优先的并行队列。相对于ParallelActionQueue 的主要区别就是减少数据在多个不同的线程之间的切换
 *
 * @author pengbingting
 */
public class FastParallelActionQueue extends AbstractActionQueue {

    private ThreadPoolExecutor singleThreadExecutor;

    public FastParallelActionQueue(ThreadPoolExecutor singleThreadExecutor, Queue<Action> queue) {
        super(queue);
        if (singleThreadExecutor.getCorePoolSize() != 1) {
            singleThreadExecutor.setCorePoolSize(1);
            singleThreadExecutor.setMaximumPoolSize(1);
            singleThreadExecutor.setKeepAliveTime(0, TimeUnit.MILLISECONDS);
        }

        this.singleThreadExecutor = singleThreadExecutor;
    }

    public FastParallelActionQueue(ThreadPoolExecutor singleThreadExecutor) {
        super(new LinkedList<>());
        if (singleThreadExecutor.getCorePoolSize() != 1) {
            singleThreadExecutor.setCorePoolSize(1);
            singleThreadExecutor.setMaximumPoolSize(1);
            singleThreadExecutor.setKeepAliveTime(0, TimeUnit.MILLISECONDS);
        }

        this.singleThreadExecutor = singleThreadExecutor;
    }

    @Override
    public void doExecute(Runnable action) {
        if (singleThreadExecutor.getQueue().isEmpty()) {
            singleThreadExecutor.execute(action);
        } else {
            singleThreadExecutor.getQueue().offer(action);
        }
    }
}
