package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.swift.proto.InteractivePayload;

/**
 *
 */
public interface IInteractive {

    default InteractivePayload getInteractivePayload() {

        return null;
    }

    boolean sendPayload(InteractivePayload raftInteractivePayload);
}
