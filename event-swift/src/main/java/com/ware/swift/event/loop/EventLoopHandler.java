package com.ware.swift.event.loop;

import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.common.Log;
import com.ware.swift.event.object.pipeline.IPipelineEventListener;

import java.util.Collection;

/**
 * @param <E>
 */
public class EventLoopHandler<E> implements IEventLoopHandler<E> {
    protected IEventLoopQueue<EventLoopHandler<E>> eventLoopQueue;
    protected Collection<IPipelineEventListener<E>> pipelineListeners;
    protected ObjectEvent<E> event;
    protected Long createTime;
    protected AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup;

    public EventLoopHandler(IEventLoopQueue<EventLoopHandler<E>> eventLoopQueue,
                            AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup,
                            Collection<IPipelineEventListener<E>> pipelineListeners,
                            ObjectEvent<E> event) {
        super();
        this.eventLoopQueue = eventLoopQueue;
        this.pipelineListeners = pipelineListeners;
        this.event = event;
        this.createTime = System.currentTimeMillis();
        this.asyncEventLoopGroup = asyncEventLoopGroup;
    }

    public EventLoopHandler(AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup,
                            Collection<IPipelineEventListener<E>> pipelineListeners,
                            ObjectEvent<E> event) {
        super();
        this.pipelineListeners = pipelineListeners;
        this.event = event;
        this.createTime = System.currentTimeMillis();
    }

    public void setEventLoopQueueIfAbsent(IEventLoopQueue<EventLoopHandler<E>> queue) {
        if (this.eventLoopQueue == null) {
            // 两层判断的目的是 不需要每次过来都获取锁释放锁
            synchronized (this) {
                if (this.eventLoopQueue == null) {
                    this.eventLoopQueue = queue;
                }
            }
        }
    }

    public ObjectEvent<E> getEvent() {
        return event;
    }

    public void setEvent(ObjectEvent<E> event) {
        this.event = event;
    }

    @Override
    public void run() {
        if (eventLoopQueue == null) {
            return;
        }

        boolean isStop = false;
        long start = System.currentTimeMillis();
        try {
            // 返回 false,表示当前这个event 还没有处理完，将等待interval时间，进入下一轮处理
            isStop = execute();
            long end = System.currentTimeMillis();
            long interval = end - start;
            long leftTime = end - createTime;
            if (interval >= 1000) {
                Log.warn("execute action : " + this.toString() + ", interval : "
                        + interval + ", leftTime : " + leftTime + ", size : "
                        + eventLoopQueue.getQueue().size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.error("run action execute exception. action : " + this.toString()
                    + e.getMessage());
        } finally {
            eventLoopQueue.dequeue(this);
            if (!isStop) {
                eventLoopQueue.schedulerEventLoopHandler(this);
            }
        }
    }

    @Override
    public boolean execute() {
        boolean isStop = false;
        int listenerIndex = 1;
        for (IPipelineEventListener<E> pipeline : pipelineListeners) {
            // get the last handler to Decide to append event loop queue
            isStop = pipeline.onEvent(event, listenerIndex);
            if (event.isInterrupt()) {
                // interruptor current pipeline object listener
                break;
            }
            listenerIndex++;
        }
        return isStop;
    }

    public AbstractAsyncEventLoopGroup<E> getAsyncEventLoopGroup() {
        return asyncEventLoopGroup;
    }

    public void setAsyncEventLoopGroup(
            AbstractAsyncEventLoopGroup<E> asyncEventLoopGroup) {
        this.asyncEventLoopGroup = asyncEventLoopGroup;
    }

}
