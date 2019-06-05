package com.ware.swift.core.remoting;

import com.ware.swift.proto.InteractivePayload;

/**
 *
 */
public interface IInteractive {

    default InteractivePayload getInteractivePayload() {

        return null;
    }

    boolean sendPayload(InteractivePayload wareSwiftInteractivePayload);
}
