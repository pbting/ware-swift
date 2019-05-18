package com.alibaba.aliware.grpc.swift.event.swift.graph;

/**
 * 如果这个节点仅仅是处理业务,不需要异步回调执行完后的结果,则实现该接口
 * @author pengbingting
 *
 */
public interface RunnableNodeCmd extends INodeCommand {

	void handler();
}
