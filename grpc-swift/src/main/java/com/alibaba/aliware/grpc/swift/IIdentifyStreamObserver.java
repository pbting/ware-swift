package com.alibaba.aliware.grpc.swift;

public interface IIdentifyStreamObserver {

	String getIndendity();

	void setIndendity(String indendity);

	AbstractCallStreamObserver getAbstractCallStreamObserver();
}
