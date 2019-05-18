package com.ware.swift.event.object.multi;

import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.object.IEventCallBack;
import com.ware.swift.event.object.IEventObjectListener;

import java.util.Deque;

import com.ware.swift.event.object.fast.AbstractFastAsyncEventObject;
import com.ware.swift.event.parallel.IParallelQueueExecutor;

/**
 * 支持多个消费者的执行模式
 * @author pengbingting
 *
 * @param <V>
 */
public abstract class AbstractMultiEventObject<V> extends AbstractFastAsyncEventObject<V> implements IMultiCastEventObject<V>{

	public AbstractMultiEventObject(IParallelQueueExecutor superFastParallelQueueExecutor, boolean isOptimism) {
		super(superFastParallelQueueExecutor, isOptimism);
	}

	public AbstractMultiEventObject(IParallelQueueExecutor superFastParallelQueueExecutor) {
		super(superFastParallelQueueExecutor);
	}

	@Override
	public void multiCast(final Deque<IEventObjectListener<V>> eventObjectListeners, final ObjectEvent<V> event) {
		IParallelQueueExecutor parallelQueueExecutor = getParallelQueueExecutor();
		for(final IEventObjectListener<V> eventObjectListener:eventObjectListeners){
			parallelQueueExecutor.executeOneTime(new Runnable() {
				@Override
				public void run() {
					try {
						eventObjectListener.onEvent(event);
					} catch (Throwable e) {
						e.printStackTrace();
					}finally{
						Object object = event.getParameter(ObjectEvent.EVENT_CALLBACK);
						if(object == null){
							
							return ;
						}
						
						if(object instanceof IEventCallBack){
							IEventCallBack iEventCallBack = (IEventCallBack) object;
							iEventCallBack.eventCallBack(event);
						}
					}
				}
			});
		}
	}

	@Override
	public abstract void attachListener() ;


	@Override
	public void listenerHandler(Deque<IEventObjectListener<V>> eventObjectListeners, ObjectEvent<V> event) {
		
		multiCast(eventObjectListeners, event);
	}
}
