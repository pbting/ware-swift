package com.alibaba.aliware.grpc.swift.event.swift.parallel.action;

import com.alibaba.aliware.grpc.swift.event.swift.common.Log;
import com.alibaba.aliware.grpc.swift.event.swift.common.Log;

/**
 * if current action must wait the prefer finished then extend this class.
 * @author pengbingting
 *
 */
public abstract class Action implements Runnable {

	protected IActionQueue<Action> queue;
	protected Long createTime;
	protected String topicName ;
	
	public Action() {
		createTime = System.currentTimeMillis();
	}
	
	public Action(IActionQueue<Action> queue) {
		this.queue = queue;
		createTime = System.currentTimeMillis();
	}
	
	public void setActionQueue(IActionQueue<Action> queue){
		this.queue = queue;
	}
	
	public IActionQueue<Action> getActionQueue() {
		return queue;
	}

	public void run() {
		if (queue != null) {
			long start = System.currentTimeMillis();
			try {
				execute();
				long end = System.currentTimeMillis();
				long interval = end - start;
				long leftTime = end - createTime;
				if (interval >= 1000) {
					Log.warn("topic anme="+topicName+"; execute action : " + this.toString() + ", interval : " + interval + ", leftTime : " + leftTime + ", size : " + queue.getQueue().size());
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.error("run action execute exception. action : " + this.toString()+e.getMessage());
			} finally {
				queue.dequeue(this);
			}
		}
	}

	public abstract void execute() throws ActionExecuteException;
	
	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public void execeptionCaught(Exception e){
		
	}
}
