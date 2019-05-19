package com.ware.swift.core.remoting;

import com.ware.swift.proto.InteractivePayload;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class HeartbeatSingleMailbox implements IMailbox<InteractivePayload> {

    private final LinkedBlockingQueue<InteractivePayload> payloadQueue = new LinkedBlockingQueue(
            1);

    private String identify;

    public HeartbeatSingleMailbox(String identify) {
        this.identify = identify;
    }

    @Override
    public void producer(InteractivePayload value) {
        if (payloadQueue.size() > 0) {
            payloadQueue.remove();
        }
        payloadQueue.add(value);
    }

    @Override
    public InteractivePayload consumer(int timeout, TimeUnit timeUnit) {
        try {
            return payloadQueue.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public String identify() {
        return this.identify;
    }
}
