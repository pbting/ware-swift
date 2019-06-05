package com.ware.swift.core.remoting;

import com.ware.swift.proto.InteractivePayload;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class HeartbeatMultiMailbox implements IMailbox<InteractivePayload> {

    private final LinkedBlockingQueue<InteractivePayload> payloadsQueue = new LinkedBlockingQueue<>();

    private String identify;

    public HeartbeatMultiMailbox(String identify) {
        this.identify = identify;
    }

    @Override
    public void producer(InteractivePayload value) {
        payloadsQueue.offer(value);
    }

    @Override
    public InteractivePayload consumer(int timeout, TimeUnit timeUnit) {
        try {
            return payloadsQueue.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public String identify() {
        return identify;
    }
}
