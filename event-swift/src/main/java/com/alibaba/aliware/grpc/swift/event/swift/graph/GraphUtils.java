package com.alibaba.aliware.grpc.swift.event.swift.graph;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GraphUtils {

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);
	
	/**
	 * if you want set the executor service then call this method
	 * @param executorService
	 */
	public static void setExecutorServer(ExecutorService executorService){
		GraphUtils.executorService = executorService;
	}
	
	/**
	 * 
	 * @param graphAction
	 */
	public static void submit(GraphRunnable graphAction){
		if(graphAction == null){
			return ;
		}
		
		executorService.execute(graphAction);
	}
	
	/**
	 * 如果是 开始节点,则调用 这个方法来触发.一次后继依赖的节点只要达到调教便可执行
	 * 
	 * @param startNode
	 * @param scheduler
	 */
	public static void triigerStartNode(Node<INodeCommand> startNode, GraphScheduler scheduler){
		GraphRunnable graphAction = new GraphRunnable(startNode, scheduler);
		submit(graphAction);
	}
	
	public static void triigerStartNode(Node<INodeCommand> startNode, Graph graph){
		GraphRunnable graphAction = new GraphRunnable(startNode, new GraphScheduler(graph));
		submit(graphAction);
	}
	
	public static void scheduler(Graph graph){
		GraphRunnable graphAction = new GraphRunnable(graph.getStartNode(), new GraphScheduler(graph));
		submit(graphAction);
	}
	
	public static void shutdown(){
		executorService.shutdown();
	}
}
