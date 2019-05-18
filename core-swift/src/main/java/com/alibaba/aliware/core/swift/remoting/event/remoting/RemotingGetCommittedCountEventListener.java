package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;

/**
 *
 */
public class RemotingGetCommittedCountEventListener
        extends AbstractRemotingPipelineEventListener {

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {

        long committedCount = RemotingManager.getRemotingManager().getRaftConfig()
                .getNodeInformation().getCommittedCount();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setPayload(ByteString
                .copyFrom(Long.toString(committedCount).getBytes(CharsetUtil.UTF_8)));
        event.getValue().sendPayload(builder.build());
        return true;
    }
}
