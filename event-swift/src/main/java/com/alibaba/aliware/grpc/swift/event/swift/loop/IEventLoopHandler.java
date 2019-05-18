package com.alibaba.aliware.grpc.swift.event.swift.loop;

interface IEventLoopHandler<T> extends Runnable {

	boolean execute();

	AbstractAsyncEventLoopGroup<T> getAsyncEventLoopGroup();

	void setAsyncEventLoopGroup(AbstractAsyncEventLoopGroup<T> asyncEventLoopGroup);
}
