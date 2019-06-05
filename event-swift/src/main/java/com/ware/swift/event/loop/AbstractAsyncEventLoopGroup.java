package com.ware.swift.event.loop;

import com.ware.swift.event.IAsyncEventObject;
import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.IEventPartitionerRegister;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.object.IEventCallBack;
import com.ware.swift.event.object.pipeline.AbstractPipelineEventObject;
import com.ware.swift.event.object.pipeline.IPipelineEventListener;
import com.ware.swift.event.parallel.IParallelQueueExecutor;
import com.ware.swift.event.parallel.SuperFastParallelQueueExecutor;
import com.ware.swift.event.parallel.action.IParallelActionExecutor;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * support for event loop groups the default event loop interval time is one seconds.
 *
 * @param <E>
 * @author pengbingting 注意：不支持后置事件处理器
 */
public abstract class AbstractAsyncEventLoopGroup<E>
        extends AbstractPipelineEventObject<E> implements IAsyncEventObject<E>, IEventPartitionerRegister {

    private IEventPartitioner iEventPartitioner;

    protected ConcurrentHashMap<Integer, EventLoopQueue<E>> eventLoopQueueGroup = new ConcurrentHashMap<>();
    protected IParallelQueueExecutor executor;
    protected long schedulerInterval;// 统一以毫秒为单位

    public AbstractAsyncEventLoopGroup(String executorName, boolean isOptimism) {
        super(isOptimism);
        int coreSize = Runtime.getRuntime().availableProcessors() * 4;
        this.executor = new SuperFastParallelQueueExecutor(coreSize, executorName);
        this.schedulerInterval = TimeUnit.SECONDS.toMillis(1);
    }

    public AbstractAsyncEventLoopGroup(IParallelQueueExecutor executor,
                                       boolean isOptimism) {
        super(isOptimism);
        this.executor = executor;
        this.schedulerInterval = TimeUnit.SECONDS.toMillis(1);
    }

    public AbstractAsyncEventLoopGroup(String executorName, long schedulerInterval) {
        super(true);
        this.schedulerInterval = schedulerInterval;
        int coreSize = Runtime.getRuntime().availableProcessors() * 4;
        this.executor = new SuperFastParallelQueueExecutor(coreSize, executorName);
    }

    public AbstractAsyncEventLoopGroup(IParallelQueueExecutor executor,
                                       long schedulerInterval) {
        super(true);
        this.schedulerInterval = schedulerInterval;
        this.executor = executor;
    }

    /**
     * then can call must add event loop queue。 更改触发的方式
     */
    @Override
    public void listenerHandler(Deque<IPipelineEventListener<E>> objectListeners,
                                ObjectEvent<E> event) {
        int eventType = event.getEventType();
        EventLoopQueue<E> eventLoopQueue = eventLoopQueueGroup.get(eventType);
        if (eventLoopQueue == null) {
            lock.lock();
            try {
                eventLoopQueue = eventLoopQueueGroup.get(eventType);
                if (eventLoopQueue == null) {
                    eventLoopQueue = new EventLoopQueue<>(executor, this);
                    eventLoopQueueGroup.put(eventType, eventLoopQueue);
                }
            } finally {
                lock.unlock();
            }
        }
        eventLoopQueue.enqueue(
                new EventLoopHandler<>(eventLoopQueue, this, objectListeners, event));
    }

    /**
     * 确定不需要用的事件类型，需要手动 remove 是一个很好的习惯
     */
    @Override
    public void removeListener(Integer eventType) {
        super.removeListener(eventType);
        eventLoopQueueGroup.remove(eventType);
    }

    public void setSchedulerInterval(int value, TimeUnit timeUnit) {

        this.schedulerInterval = timeUnit.toMillis(value);
    }

    public long getSchedulerInterval() {
        return this.schedulerInterval;
    }

    public void shutdown() {
        this.executor.stop();
    }

    @Override
    public String partitioner(ObjectEvent<E> objectEvent) {

        if (this.iEventPartitioner != null) {

            return this.iEventPartitioner.partitioner(objectEvent);
        }

        return objectEvent.getEventTopic();
    }

    @Override
    public void registerEventPartitioner(IEventPartitioner eventPartitioner) {

        this.iEventPartitioner = eventPartitioner;
    }

    /**
     * 可以进应急队列
     */
    @Override
    public void enEmergencyQueue(Runnable runnable) {

        executor.enEmergencyQueue(runnable);
    }

    @Override
    public void adjustExecutor(int coreSize, int maxSize) {
        if (this.executor instanceof IParallelActionExecutor) {
            IParallelActionExecutor parallelActionExecutor = (IParallelActionExecutor) this.executor;
            parallelActionExecutor.adjustPoolSize(coreSize, maxSize);
        }
    }

    @Override
    public void publish(E value, Integer eventType, IEventCallBack iEventCallBack) {

        notifyListeners(new ObjectEvent<>(this, value, eventType), iEventCallBack);
    }

    @Override
    public void notifyListeners(ObjectEvent<E> objectEvent,
                                IEventCallBack iEventCallBack) {
        objectEvent.setParameter(ObjectEvent.EVENT_CALLBACK, iEventCallBack);
        notifyListeners(objectEvent);
    }

    @Override
    public IParallelQueueExecutor getParallelQueueExecutor() {

        return this.executor;
    }
}
