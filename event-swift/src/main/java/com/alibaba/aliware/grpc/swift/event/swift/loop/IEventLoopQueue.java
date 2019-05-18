package com.alibaba.aliware.grpc.swift.event.swift.loop;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IActionQueue;

interface IEventLoopQueue<T extends Runnable> extends IActionQueue<T> {

	void schedulerdEventLoopHandler(T event);

	long getEventLoopInterval(T eventHandler);
}
