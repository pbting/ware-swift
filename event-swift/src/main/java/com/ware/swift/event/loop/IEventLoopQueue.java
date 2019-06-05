package com.ware.swift.event.loop;

import com.ware.swift.event.parallel.action.IActionQueue;

/**
 * @param <T>
 */
interface IEventLoopQueue<T extends Runnable> extends IActionQueue<T> {

    /**
     * @param event
     */
    void schedulerEventLoopHandler(T event);

    /**
     * @param eventHandler
     * @return
     */
    long getEventLoopInterval(T eventHandler);
}
