package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareSwiftStringUtils;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 每个节点刚启动的时候，会和配置的各个节点 say hello 一次。节点收到 say hello 的消息，会返回当前集群。
 * <p>
 * Leader-Follower：返回的是当前集群的 leader 节点。
 * <p>
 * 目的是为下一步做好准备：拿到 leader 节点之后，就开始启动 leader 节点的心跳接收
 * {@link LeaderHeartbeatTimeoutCheckEventListener}。
 * <p>
 * 注意：这里是并发的给每个节点发送 say hello 消息。
 */
public class LeaderFollowerSendSayHelloEventListener
        extends AbstractLocalPipelineEventListener<AbstractRemotingChannel> {

    private final InternalLogger log = InternalLoggerFactory
            .getInstance(LeaderFollowerSendSayHelloEventListener.class);

    public LeaderFollowerSendSayHelloEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<AbstractRemotingChannel> event,
                           int listenerIndex) {
        AbstractRemotingChannel remotingChannel = event.getValue();
        String channelIdentify = RemotingManager.getRemotingManager().getChannelIdentify(
                remotingManager.getWareSwiftConfig().getNodeInformation().identify());
        InteractivePayload response = sendSayHello(remotingChannel, channelIdentify);

        // 这里正常含有 leader 节点的信息，是一个包含 [@] 的二元符。{address:port}@{cluster_name}
        NodeInformation responseLeader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                .decodeResult(response.getPayload().toByteArray(), NodeInformation.class);

        // 说明当前很有已经选出 leader。这种情况只有在集群运行过程中新添加一个节点才会出现。
        if (WareSwiftStringUtils.isNotBlank(responseLeader.getAddressPort())) {
            processResponseLeader(remotingChannel, responseLeader);
        }

        remotingManager.increSendIdentifyCount();
        log.debug(remotingChannel.getAddressPort() + "has send success and will exit ..");
        return true;
    }

    /**
     * 处理 say hello 后接收到 leader 的情况。
     */
    private void processResponseLeader(AbstractRemotingChannel remotingChannel,
                                       NodeInformation responseLeader) {
        remotingManager.getWareSwiftConfig().setLeader(responseLeader);
        log.debug("say hello get leader [" + responseLeader.identify() + "] from "
                + remotingChannel.identify() + " node.");
        remotingManager.getWareSwiftConfig().getSayHelloResponseLeaderCount()
                .incrementAndGet();

        if (remotingManager.getWareSwiftConfig().getLeader() != null
                && !remotingManager.getWareSwiftConfig().getLeader().identify()
                .equalsIgnoreCase(responseLeader.identify())) {
            // 如果是基于 Leader-Follower 架构，系统不允许同时出现两个不同的 leader。直接退出
            log.error("cluster has two leader. one is :"
                    + remotingManager.getWareSwiftConfig().getLeader().identify()
                    + "; and the other is :" + responseLeader.identify());
            System.exit(1);
        }

        // 判断当前节点是否已经和 leader 建立连接。有可能给出的节点列表中不包含当前的 leader 信息。
        String leaderIdentify = RemotingManager.getRemotingManager()
                .getChannelIdentify(responseLeader.identify());
        if (!remotingManager.isContainsRemotingChannel(leaderIdentify)) {
            log.info("初始化和 leader 建立连接:" + RemotingManager.getRemotingManager()
                    .getChannelIdentify(responseLeader.identify()));
            AbstractRemotingChannel leaderRemotingChannel = RemotingManager
                    .getRemotingManager().getRemotingChannelFactory()
                    .newRemotingChannel(responseLeader.getAddressPort(),
                            responseLeader.getClusterName(), AbstractRemotingChannel.JoinType.NODE_MEET);
            remotingManager.addRemotingChannel(leaderRemotingChannel);
        }
    }

    /**
     * @param remotingChannel
     * @param source
     * @return
     */
    public static InteractivePayload sendSayHello(AbstractRemotingChannel remotingChannel,
                                                  String source) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setEmmitTime(System.currentTimeMillis());
        builder.setEventType(RemotingEventDispatcher.REMOTING_SAY_HELLO_EVENT_TYPE);
        builder.setSource(source);
        builder.setSink(remotingChannel.identify());
        // 使用 Grpc 的 rpc 调用范式来发起一个远程的调用。
        return remotingChannel.requestResponse(builder.build());
    }
}
