package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;

/**
 * 接收新加入的节点，和 当前此节点(有可能是 leader/PTP) 进行 meet 操作。
 * <p>
 * 进入此逻辑，说明节点已经超时没有收到 leader/PTP 的心跳，并且同时已经判断 leader/PTP 当前的状态是存活的。
 * <p>
 * 适用于 Leader-Follower/去中心化 的架构
 */
public class RemotingNodeMeetEventListener extends AbstractRemotingPipelineEventListener {

    public RemotingNodeMeetEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        try {
            NodeInformation nodeInformation = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(event.getValue().getInteractivePayload().getPayload()
                            .toByteArray(), NodeInformation.class);
            // 1、节点的 remoting channel 注册表中需要新增加
            AbstractRemotingChannel remotingChannel = AbstractRemotingChannel.addNewNode(
                    nodeInformation.getAddressPort(), nodeInformation.getClusterName());
            // 2、告诉新增加的节点当前集群中有哪些节点。
            onSendAllRemotingChannels(event.getValue());
            // 3、启动给这个新加入的节点发送心跳
            RemotingManager.getRemotingManager().sendHeartbeats(remotingChannel);
            // 4、给其他节点广播有新节点增加
            RemotingManager.getRemotingManager().broadcastNewNode(nodeInformation);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 接收，无需下一次 event loop 处理。
        return false;
    }
}
