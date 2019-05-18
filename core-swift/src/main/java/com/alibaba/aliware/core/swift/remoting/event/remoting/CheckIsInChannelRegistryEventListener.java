package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
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
