package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.object.pipeline.IPipelineEventListener;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;

import java.util.Collection;

/**
 *
 */
public abstract class AbstractRemotingPipelineEventListener
        implements IPipelineEventListener<IInteractive> {

    public AbstractRemotingPipelineEventListener() {
    }

    /**
     * 将当前节点所有已经连接的 channel 发送给请求的节点
     */
    public void onSendAllRemotingChannels(IInteractive raftInteractive) throws Exception {

        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSource(raftInteractive.getInteractivePayload().getSink());
        builder.setSink(raftInteractive.getInteractivePayload().getSource());
        builder.setPayload(ByteString.copyFrom(builderRemotingChannels(
                raftInteractive.getInteractivePayload().getSource()).toString()
                .getBytes(CharsetUtil.UTF_8)));
        // response
        raftInteractive.sendPayload(builder.build());
    }

    public StringBuilder builderRemotingChannels(String source) {
        Collection<AbstractRemotingChannel> remotingChannels = RemotingManager
                .getRemotingManager().getRemotingChannels();

        final StringBuilder remotingChannelsSB = new StringBuilder();
        remotingChannels.forEach(remotingChannel -> {
            remotingChannelsSB.append(remotingChannel.identify());
            remotingChannelsSB.append("|");// 竖线分隔多个节点。
        });

        return remotingChannelsSB;
    }
}
