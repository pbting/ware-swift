package com.alibaba.aliware.grpc.swift.event.swift.parallel;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public interface IParallelQueueExecutor extends Executor {

	/**
	 * 
	 * @param command
	 */
	void enEmergenceyQueue(Runnable command);

	/**
	 * 
	 * @param topic
	 * @param command
	 */
	void execute(String topic, Runnable command);

	/**
	 * 执行一次性的 runnable
	 */
	void executeOneTime(Runnable command);

	/**
	 * 
	 */
	void stop();

	/**
	 * 
	 * @return
	 */
	ScheduledExecutorService getScheduledExecutorService();

	/**
	 * 
	 * @param topics
	 */
	void registerTopics(String... topics);

	/**
	 * 
	 * @param topic
	 */
	void removeTopic(String topic);

	/**
	 * 
	 * @param isAuto 是否系统触发
	 */
	void cronTrriger(boolean isAuto);

}
