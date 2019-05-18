package com.alibaba.aliware.grpc.swift.event.swift.parallel;

import java.io.Serializable;


public class ThreadPoolContainer implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int threadSize ; //线程池中活动线程池大小
	private int largestPoolSize; //有史以来的峰值大小
	private int activeTaskCount;// 正在执行的任务数
	private int waitTaskCount ; //在队列中等待执行的任务数
	private int completeTaskCount ;//已经调度完成的 task count 
	private int taskCount ;//总的被调度的task count = complete + working + in queue
	public int getThreadSize() {
		return threadSize;
	}
	public void setThreadSize(int threadSize) {
		this.threadSize = threadSize;
	}
	public int getLargestPoolSize() {
		return largestPoolSize;
	}
	public void setLargestPoolSize(int largestPoolSize) {
		this.largestPoolSize = largestPoolSize;
	}
	public int getActiveTaskCount() {
		return activeTaskCount;
	}
	public void setActiveTaskCount(int activeTaskCount) {
		this.activeTaskCount = activeTaskCount;
	}
	public int getWaitTaskCount() {
		return waitTaskCount;
	}
	public void setWaitTaskCount(int waitTaskCount) {
		this.waitTaskCount = waitTaskCount;
	}
	public int getCompleteTaskCount() {
		return completeTaskCount;
	}
	public void setCompleteTaskCount(int completeTaskCount) {
		this.completeTaskCount = completeTaskCount;
	}
	public int getTaskCount() {
		return taskCount;
	}
	public void setTaskCount(int taskCount) {
		this.taskCount = taskCount;
	}
	@Override
	public String toString() {
		return "ThreadPoolContainer [threadSize=" + threadSize
				+ ", largestPoolSize=" + largestPoolSize + ", activeTaskCount="
				+ activeTaskCount + ", waitTaskCount=" + waitTaskCount
				+ ", completeTaskCount=" + completeTaskCount + ", taskCount="
				+ taskCount + "]";
	}

}
