package com.ware.swift.core.remoting.event.local;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.loop.EventLoopConstants;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 每个节点都需要感知其他节点是否是 online 还是 offline。
 * <p>
 * Leader-Follower：如果 Follower 节点在指定的超时时间内，没有接收到 Leader 发送的心跳，则会重新开启一次选举。
 */
public class LeaderHeartbeatTimeoutCheckEventListener
        extends NodeTimeoutCheckEventListenerSupport<IMailbox<InteractivePayload>> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(LeaderHeartbeatTimeoutCheckEventListener.class);

    public LeaderHeartbeatTimeoutCheckEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<IMailbox<InteractivePayload>> event,
                           int listenerIndex) {

        event.setParameter(EventLoopConstants.EVENT_LOOP_INTERVAL_PARAM,
                TimeUnit.SECONDS.toMillis(1));

        Long checkCount = event
                .getParameter(RemotingInteractiveConstants.HEARTBEAT_CHECK_COUNT);

        if (checkCount == null) {
            event.setParameter(RemotingInteractiveConstants.HEARTBEAT_CHECK_COUNT, 0);
            checkCount = 0l;
        }

        event.setParameter(RemotingInteractiveConstants.HEARTBEAT_CHECK_COUNT,
                checkCount++);

        IMailbox<InteractivePayload> mailbox = event.getValue();
        long s = System.currentTimeMillis();
        InteractivePayload heartbeatPayload = mailbox.consumer(10, TimeUnit.MILLISECONDS);
        if (heartbeatPayload != null) {
            logger.debug(String.format(
                    "cost time [%s] ms to wait leader send information with heartbeat.",
                    (System.currentTimeMillis() - s)));
            processHeartbeatPayload(heartbeatPayload);
            return false;
        }
        // case 1:处理一次选举没有选举成功的情况
        if (remotingManager.getRaftConfig().getLeader() == null) {
            // do something
            RemotingManager.getRemotingManager().reElectionForLeader();
            return true;
        }

        if (remotingManager.getRaftConfig().getLeader().identify().equalsIgnoreCase(
                remotingManager.getRaftConfig().getNodeInformation().identify())) {
            return true;
        }

        Long heartbeatTimeoutCount = event
                .getParameter(RemotingInteractiveConstants.HEARTBEAT_TIMEOUT_COUNT, 1l);
        if (heartbeatTimeoutCount <= 3) {
            heartbeatTimeoutCount++;
            event.setParameter(RemotingInteractiveConstants.HEARTBEAT_TIMEOUT_COUNT,
                    heartbeatTimeoutCount);
            return false;
        }
        event.removeParameter(RemotingInteractiveConstants.HEARTBEAT_TIMEOUT_COUNT);
        // 没有收到心跳信息，有两种情况，一种情况是：leader 已经挂了。另外一种情况是：新加入的节点 leader 没有被感知。
        // 在已经产生 leader 的情况，超时了。
        AbstractRemotingChannel leaderChannel = remotingManager
                .getRemotingChannel(remotingManager.getChannelIdentify(
                        remotingManager.getRaftConfig().getLeader().identify()));

        String selfIdentify = remotingManager.getChannelIdentify(
                remotingManager.getRaftConfig().getNodeInformation().identify());
        try {
            InteractivePayload responsePayload = leaderChannel
                    .requestResponse(InteractivePayload.newBuilder().setEventType(
                            RemotingEventDispatcher.REMOTING_CHECK_IS_IN_CHANNEL_REGISTRY_EVENT_TYPE)
                            .setSource(selfIdentify)
                            .setPayload(ByteString
                                    .copyFrom(selfIdentify.getBytes(CharsetUtil.UTF_8)))
                            .build());
            System.err.println(String.format("double check leader is contains %s:%s",
                    selfIdentify, responsePayload.getPayload().toStringUtf8()));
            // case 1: 新加入的节点：
            // is active，说明 leader 列表中没有当前这个节点，需要进行一次 leader meet
            if (!Boolean.valueOf(responsePayload.getPayload().toStringUtf8())) {
                remotingManager.getEventLoopGroup().publish(leaderChannel,
                        AbstractRemotingManager.NODE_MEET_EVENT_TYPE);
            } else {
                // case 2: offline -> online。sendSayHello 会处理重新上线的节点，并且 leader 又会重新开始发送心跳
                LeaderFollowerSendSayHelloEventListener.sendSayHello(leaderChannel,
                        remotingManager.getChannelIdentify(remotingManager.getRaftConfig()
                                .getNodeInformation().identify()));
            }
        } catch (Exception e) {
            // 如果发生网络异常，则直接退出当前 Leader 节点的心跳超时检测。会在重新选举 Leader 后再一次重新触发这里的超时检测任务。
            return onException(e, leaderChannel);
        }
        // leader 节点会晤之后，正常情况下就应该可以收到 leader 发送过来的心跳了。因此这里不需要退出循环。
        return false;
    }

    public boolean onException(Exception e,
                               AbstractRemotingChannel abstractRemotingChannel) {
        if (abstractRemotingChannel != null
                && abstractRemotingChannel.isNetUnavailable(e)) {
            abstractRemotingChannel.openOnlineCheckStatus();
            RemotingManager.getRemotingManager().reElectionForLeader();
            return true;
        }

        return false;
    }
}
