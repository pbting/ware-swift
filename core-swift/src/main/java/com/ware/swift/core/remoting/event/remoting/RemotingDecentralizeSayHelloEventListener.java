package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.IDecentralizeRemotingManager;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;

/**
 * 处理去中心化集群发送 say hello 的 event listener。
 */
public class RemotingDecentralizeSayHelloEventListener
        extends AbstractRemotingPipelineEventListener {

    public RemotingDecentralizeSayHelloEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive wareSwiftInteractive = event.getValue();
        InteractivePayload wareSwiftInteractivePayload = wareSwiftInteractive
                .getInteractivePayload();

        // 不做身份节点身份校验
        if (!wareSwiftInteractivePayload.getSource()
                .startsWith(RemotingInteractiveConstants.PTP_ROLE_PREFIX)) {
            RemotingManager.getRemotingManager()
                    .isOnlineWithRemotingChannel(wareSwiftInteractivePayload.getSource());
            wareSwiftInteractive.sendPayload(InteractivePayload.newBuilder()
                    .setSource(wareSwiftInteractivePayload.getSink())
                    .setSink(wareSwiftInteractivePayload.getSink())
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

        return IDecentralizeRemotingManager.DecentralizeRemotingManager.DEFAULT_IMPL.builderRemotingChannels(source);
    }
}
