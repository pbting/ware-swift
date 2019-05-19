package com.ware.swift.event.loop;

import com.ware.swift.event.parallel.action.IActionQueue;

interface IEventLoopQueue<T extends Runnable> extends IActionQueue<T> {

	void schedulerdEventLoopHandler(T event);

	long getEventLoopInterval(T eventHandler);
}
