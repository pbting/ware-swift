package com.ware.swift.event.graph.future;

import com.ware.swift.event.graph.INodeCommand;

/**
 * 提供异步可回调的 graph 节点 command.我们建议最好是在构造函数中就把 FutureNodeCmd 的 instance 给传递过来
 * @author pengbingting
 *
 */
public interface CallableNodeCmd<T> extends INodeCommand {

	T call() throws Exception;

	FutureResult<T> getFutureResult();

	T get();
}
