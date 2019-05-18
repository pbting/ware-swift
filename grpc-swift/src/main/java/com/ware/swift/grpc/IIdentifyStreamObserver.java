package com.ware.swift.grpc;

public interface IIdentifyStreamObserver {

	String getIndendity();

	void setIndendity(String indendity);

	AbstractCallStreamObserver getAbstractCallStreamObserver();
}
