package com.ware.swift.event.loop;

/**
 * @param <T>
 */
interface IEventLoopHandler<T> extends Runnable {

    /**
     * @return
     */
    boolean execute();

    /**
     * @return
     */
    AbstractAsyncEventLoopGroup<T> getAsyncEventLoopGroup();

    /**
     * @param asyncEventLoopGroup
     */
    void setAsyncEventLoopGroup(AbstractAsyncEventLoopGroup<T> asyncEventLoopGroup);
}
