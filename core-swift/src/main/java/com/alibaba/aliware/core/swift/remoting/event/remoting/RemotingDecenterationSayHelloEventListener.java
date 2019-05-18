package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;

import java.util.Collection;

/**
 * 处理去中心化集群发送 say hello 的 event listener。
 */
public class RemotingDecenterationSayHelloEventListener
        extends AbstractRemotingPipelineEventListener {

    public RemotingDecenterationSayHelloEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive raftInteractive = event.getValue();
        InteractivePayload raftInteractivePayload = raftInteractive
                .getInteractivePayload();

        // 不做身份节点身份校验
        if (!raftInteractivePayload.getSource()
                .startsWith(RemotingInteractiveConstants.PTP_ROLE_PRIFIX)) {
            RemotingManager.getRemotingManager()
                    .isOnlineWithRemotingChannel(raftInteractivePayload.getSource());
            raftInteractive.sendPayload(InteractivePayload.newBuilder()
                    .setSource(raftInteractivePayload.getSink())
                    .setSink(raftInteractivePayload.getSink())
                    .setPayload(
                            ByteString.copyFrom(RemotingInteractiveConstants.ROLE_MISMATCH
                                    .getBytes(CharsetUtil.UTF_8)))
                    .build());
            return true;
        }

        try {
            onSendAllRemotingChannels(event.getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 去中心化 say hello 发送的节点信息应该是除去 channel 列表中包含 source 的以及加上本节点。
     *
     * @return
     */
    @Override
    public StringBuilder builderRemotingChannels(String source) {
        Collection<AbstractRemotingChannel> remotingChannels = RemotingManager
                .getRemotingManager().getRemotingChannels();

        final StringBuilder remotingChannelsSB = new StringBuilder();
        remotingChannels.forEach(remotingChannel -> {
            if (remotingChannel.identify().equalsIgnoreCase(source)) {
                return;
            }

            remotingChannelsSB.append(remotingChannel.identify());
            remotingChannelsSB.append("|");// 竖线分隔多个节点。
        });

        // 加上本节点。
        remotingChannelsSB.append(RemotingManager.getRemotingManager()
                .getChannelIdentify(RemotingManager.getRemotingManager().getRaftConfig()
                        .getNodeInformation().identify()));

        return remotingChannelsSB;
    }
}
