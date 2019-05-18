package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.local.DecentrationInitRemotingChannelEventListener;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;

import com.ware.swift.event.ObjectEvent;

import java.util.Collection;

/**
 * 处理去中心化发送过来的 node meet 的操作
 */
public class RemotingDecentrationNodeMeetEventListener
        extends AbstractRemotingPipelineEventListener {

    public RemotingDecentrationNodeMeetEventListener() {
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {

        try {
            // 判断节点是否已经在 remoting channel 列表中
            if (RemotingManager.getRemotingManager().isContainsRemotingChannel(
                    event.getValue().getInteractivePayload().getSource())) {
                // 直接返回当前集群中所有的节点列表。
                onSendAllRemotingChannels(event.getValue());
                return true;
            }

            NodeInformation nodeInformation = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(event.getValue().getInteractivePayload().getPayload()
                            .toByteArray(), NodeInformation.class);
            // 1、节点的 remoting channel 注册表中需要新增加
            AbstractRemotingChannel remotingChannel = AbstractRemotingChannel.addNewNode(
                    nodeInformation.getAddressPort(), nodeInformation.getClusterName());
            // 2、告诉新增加的节点当前集群中有哪些节点。
            onSendAllRemotingChannels(event.getValue());
            // 3、启动给这个新加入的节点发送心跳
            new DecentrationInitRemotingChannelEventListener(
                    RemotingManager.getRemotingManager())
                    .onAfterNewRemotingChannel(remotingChannel);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 接收，无需下一次 event loop 处理。
        return false;
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
