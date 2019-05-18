package com.alibaba.aliware.grpc.swift.event.swift;

import java.util.ArrayList;
import java.util.List;

/**
 * 并行队列执行器 builder 时的堆栈跟踪
 * @author pengbingting
 *
 */
public class BuilderStackTrace {

	private String threadPrefix ;
	private int corePoolSize,maxPoolSize ;
	private List<String> stackInfo = new ArrayList<>();
	
	public List<String> getStackInfo() {
		return stackInfo;
	}

	public void addStackInfo(String stackInfo) {
		this.stackInfo.add(stackInfo);
	}

	public String getThreadPrefix() {
		return threadPrefix;
	}

	public void setThreadPrefix(String threadPrefix) {
		this.threadPrefix = threadPrefix;
	}

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

}
