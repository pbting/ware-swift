package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;

/**
 *
 */
public class RemotingGetCommittedCountEventListener
        extends AbstractRemotingPipelineEventListener {

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {

        long committedCount = RemotingManager.getRemotingManager().getWareSwiftConfig()
                .getNodeInformation().getCommittedCount();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setPayload(ByteString
                .copyFrom(Long.toString(committedCount).getBytes(CharsetUtil.UTF_8)));
        event.getValue().sendPayload(builder.build());
        return true;
    }
}
