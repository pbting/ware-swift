package com.ware.swift.rsocket.handler;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.rsocket.Payload;

/**
 * 
 */
public abstract class AbstractRequestChannelHandler implements Publisher<Payload> {

	@Override
	public void subscribe(Subscriber<? super Payload> s) {
		s.onNext(response());
	}

	/**
	 * request channel will response some payload.then must implement this method.
	 * @return
	 */
	public abstract Payload response();

	/**
	 * request channel will receive payload,so you will processor by this method.
	 * @param payload
	 */
	public abstract void processPayload(Payload payload);
}
