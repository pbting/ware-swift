package com.alibaba.aliware.rsocket.swift;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import reactor.core.publisher.ReplayProcessor;

/**
 * 基于 RSocket 实现的通信模型
 */
public class RSocketInteractiveImpl implements IInteractive {

    private InteractivePayload interactivePayload;
    private ReplayProcessor replayProcessor;

    private volatile boolean isDroppted = false;

    public RSocketInteractiveImpl(InteractivePayload interactivePayload,
                                  ReplayProcessor replayProcessor) {
        this.interactivePayload = interactivePayload;
        this.replayProcessor = replayProcessor;
    }

    @Override
    public InteractivePayload getInteractivePayload() {
        return interactivePayload;
    }

    /**
     * @param interactivePayload
     * @return
     */
    @Override
    public boolean sendPayload(InteractivePayload interactivePayload) {
        replayProcessor.onNext(interactivePayload.toByteArray());
        return !isDroppted;
    }
}
