package com.ware.swift.grpc;

/**
 *
 */
public interface IIdentifyStreamObserver {

    String getIdentify();

    void setIdentify(String indendity);

    AbstractCallStreamObserver getAbstractCallStreamObserver();
}
