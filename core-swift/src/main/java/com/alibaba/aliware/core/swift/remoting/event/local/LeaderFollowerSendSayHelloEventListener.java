package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.WareCoreSwiftStringUtils;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
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
                remotingManager.getRaftConfig().getNodeInformation().identify());
        InteractivePayload response = sendSayHello(remotingChannel, channelIdentify);

        // 这里正常含有 leader 节点的信息，是一个包含 [@] 的二元符。{address:port}@{cluster_name}
        NodeInformation responseLeader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                .decodeResult(response.getPayload().toByteArray(), NodeInformation.class);

        // 说明当前很有已经选出 leader。这种情况只有在集群运行过程中新添加一个节点才会出现。
        if (WareCoreSwiftStringUtils.isNotBlank(responseLeader.getAddressPort())) {
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
        remotingManager.getRaftConfig().setLeader(responseLeader);
        log.debug("say hello get leader [" + responseLeader.identify() + "] from "
                + remotingChannel.identify() + " node.");
        remotingManager.getRaftConfig().getSayHelloResponseLeaderCount()
                .incrementAndGet();

        if (remotingManager.getRaftConfig().getLeader() != null
                && !remotingManager.getRaftConfig().getLeader().identify()
                .equalsIgnoreCase(responseLeader.identify())) {
            // 如果是基于 Leader-Follower 架构，系统不允许同时出现两个不同的 leader。直接退出
            log.error("cluster has two leader. one is :"
                    + remotingManager.getRaftConfig().getLeader().identify()
                    + "; and the other is :" + responseLeader.identify());
            System.exit(1);
        }

        // 判断当前节点是否已经和 leader 建立连接。有可能给出的节点列表中不包含当前的 leader 信息。
        String leaderIdentify = RemotingManager.getRemotingManager()
                .getChannelIdentify(responseLeader.identify());
        if (!remotingManager.isContainsRemotingChannel(leaderIdentify)) {
            System.err.println("初始化和 leader 建立连接:" + RemotingManager.getRemotingManager()
                    .getChannelIdentify(responseLeader.identify()));
            AbstractRemotingChannel leaderRemotingChannel = RemotingManager
                    .getRemotingManager().getRemotingChannelFactory()
                    .newRemotingChannel(responseLeader.getAddressPort(),
                            responseLeader.getClusterName());
            remotingManager.addRemotingChannel(leaderRemotingChannel);
        }
        // 在 say hello 阶段收到当前集群的 Leader，说明当前这个节点是 offline -> online 或者新加入的一个节点。处理数据同步。
        if (remotingManager.getRaftConfig().getSayHelloResponseLeaderCount().get() == 1) {
            remotingManager.getEventLoopGroup().getParallelQueueExecutor()
                    .executeOneTime(() -> ClusterDataSyncManager.startDataSyncing(
                            remotingManager.getRemotingChannel(leaderIdentify)));
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
        builder.setEmmitTime(String.valueOf(System.currentTimeMillis()));
        builder.setEventType(RemotingEventDispatcher.REMOTING_SAY_HELLO_EVENT_TYPE);
        builder.setSource(source);
        builder.setSink(remotingChannel.identify());
        // 使用 Grpc 的 rpc 调用范式来发起一个远程的调用。
        return remotingChannel.requestResponse(builder.build());
    }
}
