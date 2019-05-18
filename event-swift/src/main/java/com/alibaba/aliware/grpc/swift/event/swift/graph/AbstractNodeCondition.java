package com.alibaba.aliware.grpc.swift.event.swift.graph;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.aliware.grpc.swift.event.swift.BaseCondition;
import com.alibaba.aliware.grpc.swift.event.swift.BaseCondition;

public abstract class AbstractNodeCondition extends BaseCondition<Node<INodeCommand>, Set<Node<INodeCommand>>> {

	protected GraphScheduler scheduler ;
	protected ConcurrentHashMap<Node<INodeCommand>,AtomicInteger> messageCountMap = null;
	protected int messageTotal = 0;
	
	/**
	 * 
	 * @param schedule
	 * @param observiable graph 中 当前的这个 node
	 * @param v
	 */
	public AbstractNodeCondition(GraphScheduler schedule, Node<INodeCommand> observiable, Set<Node<INodeCommand>> v) {
		super(observiable, v);
		this.scheduler = schedule ;
		if(v!=null){
			this.messageCountMap = new ConcurrentHashMap<Node<INodeCommand>, AtomicInteger>(v.size());
			this.messageTotal = v.size();
			Iterator<Node<INodeCommand>> iter = getValue().iterator();
			while(iter.hasNext()){
				Node<INodeCommand> tmp = iter.next();
				messageCountMap.put(tmp, new AtomicInteger(0));
			}
		}
	}

	@Override
	public abstract boolean isFinished() ;

	public abstract void increMessageCount(Node<INodeCommand> targetJobNode);
	
	@Override
	public void handler() {
		if(this.isFinished()){
			//一次消息消费成功，对每一个前驱节点的消息数减一。
			for(AtomicInteger value : messageCountMap.values()){
				if(value.get()>=1){
					value.decrementAndGet();
				}
			}
			Node<INodeCommand> observiable = getObserviable();
			GraphUtils.submit(new GraphRunnable(observiable, scheduler));
		}
	}
}
