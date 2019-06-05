package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;

public class CheckIsInChannelRegistryEventListener
        extends AbstractRemotingPipelineEventListener {

    public CheckIsInChannelRegistryEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {

        String identify = event.getValue().getInteractivePayload().getPayload()
                .toStringUtf8();

        boolean isIn = RemotingManager.getRemotingManager()
                .isContainsRemotingChannel(identify);

        event.getValue()
                .sendPayload(InteractivePayload.newBuilder()
                        .setPayload(ByteString.copyFrom(
                                Boolean.toString(isIn).getBytes(CharsetUtil.UTF_8)))
                        .build());
        return true;
    }
}
