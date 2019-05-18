package com.ware.swift.event.loop;

interface IEventLoopHandler<T> extends Runnable {

	boolean execute();

	AbstractAsyncEventLoopGroup<T> getAsyncEventLoopGroup();

	void setAsyncEventLoopGroup(AbstractAsyncEventLoopGroup<T> asyncEventLoopGroup);
}
