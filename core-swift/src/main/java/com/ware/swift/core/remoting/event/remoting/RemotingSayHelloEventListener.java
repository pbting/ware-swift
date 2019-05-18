package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;

/**
 * 核心处理逻辑：
 * <p>
 * 1、需要获取客户端上报上来的 identify,同时给 client stream observer 赋值。
 * <p>
 * 2、say hello 也说明 down 的节点 online,因此也会将该节点的状态值从 isOnlineCheck(true) ->
 * isOnlineCheck(false)
 */
public class RemotingSayHelloEventListener extends AbstractRemotingPipelineEventListener {

    public RemotingSayHelloEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {

        InteractivePayload raftInteractivePayload = event.getValue()
                .getInteractivePayload();
        // 回复当前节点的信息
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSource(RemotingManager.getRemotingManager().getRaftConfig()
                .getNodeInformation().identify());
        builder.setSink(raftInteractivePayload.getSource());
        NodeInformation leader = RemotingManager.getRemotingManager().getRaftConfig()
                .getLeader();
        RemotingManager.getRemotingManager()
                .isOnlineWithRemotingChannel(raftInteractivePayload.getSource());
        leader = (leader == null ? new NodeInformation() : leader);
        builder.setPayload(
                ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .encodingResult(leader).array()));
        try {
            return event.getValue().sendPayload(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
