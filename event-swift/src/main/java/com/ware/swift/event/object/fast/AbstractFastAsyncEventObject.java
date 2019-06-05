package com.ware.swift.event.object.fast;

import com.ware.swift.event.IAsyncEventObject;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.object.AbstractEventObject;
import com.ware.swift.event.object.IEventCallBack;
import com.ware.swift.event.object.IEventObjectListener;
import com.ware.swift.event.parallel.IParallelQueueExecutor;

import java.util.Deque;

public abstract class AbstractFastAsyncEventObject<V> extends AbstractEventObject<V>
        implements IAsyncEventObject<V> {

    protected DefaultAsyncEventObjectImpl<V> defaultAsyncEventObject;

    public AbstractFastAsyncEventObject(
            IParallelQueueExecutor superFastParallelQueueExecutor) {
        this(superFastParallelQueueExecutor, true);
    }

    public AbstractFastAsyncEventObject(
            IParallelQueueExecutor superFastParallelQueueExecutor, boolean isOptimism) {
        super(isOptimism);
        this.defaultAsyncEventObject = new DefaultAsyncEventObjectImpl<>(
                superFastParallelQueueExecutor, this);
    }

    @Override
    public void shutdown() {

        defaultAsyncEventObject.shutdown();
    }

    @Override
    public void adjustExecutor(int coreSize, int maxSize) {

        defaultAsyncEventObject.adjustExecutor(coreSize, maxSize);
    }

    @Override
    public void enEmergencyQueue(Runnable runnable) {

        defaultAsyncEventObject.enEmergencyQueue(runnable);
    }

    /**
     * 改造为异步
     */
    @Override
    public void listenerHandler(final Deque<IEventObjectListener<V>> eventObjectListeners,
                                final ObjectEvent<V> event) {
        getParallelQueueExecutor().execute(partitioner(event), new Runnable() {
            @Override
            public void run() {

                doListenerHandler(eventObjectListeners, event);
            }
        });
    }

    @Override
    public String partitioner(ObjectEvent<V> event) {

        return defaultAsyncEventObject.partitioner(event);
    }

    @Override
    public IParallelQueueExecutor getParallelQueueExecutor() {

        return defaultAsyncEventObject.getParallelQueueExecutor();
    }

    @Override
    public void publish(V value, Integer eventType, IEventCallBack iEventCallBack) {

        defaultAsyncEventObject.publish(value, eventType, iEventCallBack);
    }

    @Override
    public void notifyListeners(ObjectEvent<V> objectEvent,
                                IEventCallBack iEventCallBack) {

        defaultAsyncEventObject.notifyListeners(objectEvent, iEventCallBack);
    }

    @Override
    public abstract void attachListener();
}
