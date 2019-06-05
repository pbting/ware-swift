/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ware.swift.core.remoting.event.local;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Hashtable;
import java.util.TreeMap;

/**
 * @author pbting
 * @date 2019-05-26 9:06 AM
 */
public class DecentralizeSendHeartbeatsEventListener extends SendHeartbeatsEventListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DecentralizeSendHeartbeatsEventListener.class);

    private static final Hashtable<String, NodeInformation> cluterNodeInformationRegistry = new Hashtable<>();

    public DecentralizeSendHeartbeatsEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannel> event, int listenerIndex) {
        logger.info("[ Decentralize ] send heartbeat node:" + remotingManager.getWareSwiftConfig().getNodeInformation().toString());
        eventPerform(event);
        return false;
    }

    @Override
    public void processException(Exception e, AbstractRemotingChannel remotingChannel) {
        logger.error("request/response send heartbeat cause an exception", e);
        // 确实断开连接
        if (!remotingChannel.isNetUnavailable(e)) {
            return;
        }
        remotingChannel.openOnlineCheckStatus();

        if (RemotingManager.getRemotingManager()
                .getWareSwiftConfig()
                .getNodeInformation().getCommittedCount() < 1) {
            return;
        }

        // 开始和其他节点协商出一个 Replication Controller.很简单看当前集群节点中谁的 term 值大，谁就是 Replication Controller.
        final TreeMap<Long, String> remotingChannelTerm = new TreeMap<>();
        IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        remotingManager.getRemotingChannels().forEach(remotingChannelEle -> {
            if (remotingChannelEle.isOpenOnlineCheck()) {
                // skip online check
                return;
            }
            long term = IDecentralizeRemotingManager.DecentralizeRemotingManager.DEFAULT_IMPL.generatorTerm(remotingChannelEle.identify());
            remotingChannelTerm.put(term, remotingChannelEle.identify());
        });
        String selfIdentify = remotingManager
                .getChannelIdentify(remotingManager.getWareSwiftConfig().getNodeInformation().identify());
        long selfTerm = IDecentralizeRemotingManager.DecentralizeRemotingManager.DEFAULT_IMPL.generatorTerm(selfIdentify);
        remotingChannelTerm.put(selfTerm, selfIdentify);
        // 取最大的值，如果是本身节点，就有当前节点作为数据复制节点的对象。

        final String maxIdentify = remotingChannelTerm.lastEntry().getValue();
        if (selfIdentify.equals(maxIdentify) && remotingChannel.startDataReplication()) {
            logger.info(String.format("[ Decentralize ] start data stream replication by [%s], term is [%d].", selfIdentify, selfTerm));
            // 同时由当前这个节点通知其他节点进入数据写入 backlog queue 中
            final ByteString payload = ByteString.copyFrom(
                    ClusterDataSyncManager.DATA_STREAM_REPLICATION_REASON_PHRASE_START.getBytes(CharsetUtil.UTF_8));
            remotingManager.getRemotingChannels().forEach(remotingChannelEle -> {
                if (remotingChannelEle.isOpenOnlineCheck()) {
                    return;
                }
                try {
                    InteractivePayload response = remotingChannelEle.requestResponse(
                            InteractivePayload.newBuilder().setSource(maxIdentify)
                                    .setSink(remotingChannelEle.identify())
                                    .setEventType(RemotingEventDispatcher.REMOTING_CONTROLLER_BACKLOG_QUEUE_EVENT_TYPE)
                                    .setPayload(payload).build());
                    logger.info(response.getPayload().toStringUtf8());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
        }
    }

    @Override
    public void customerBuilder(InteractivePayload.Builder builder, AbstractRemotingChannel remotingChannel) {
        IRemotingManager remotingManager = RemotingManager.getRemotingManager();
        // 1. 设置 去中心化架构下的 term 值
        builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_TERM_VALUE,
                String.valueOf(remotingManager.getWareSwiftConfig()
                        .getNodeInformation().getDecentralizeTerm()));
        // 2. 设置事件类型
        builder.setEventType(
                RemotingEventDispatcher.REMOTING_RECEIVE_DECENTRA_HEARTBEAT_EVENT_TYPE);
        super.customerBuilder(builder, remotingChannel);
    }

    @Override
    public void onSendHeartbeatSuccess(AbstractRemotingChannel remotingChannel, InteractivePayload response) {
        // 解析出通过心跳感知到的其他节点。
        String clusterNodes = response.getHeadersMap().get(RemotingInteractiveConstants.HEADER_KEY_CLUSTER_NODES);
        logger.info(String.format("[Decentralize] aware the cluster nodes[%s] by heartbeat。", clusterNodes));
        RemotingManager.getRemotingManager().processClusterNodes(clusterNodes);
        super.onSendHeartbeatSuccess(remotingChannel, response);
    }
}