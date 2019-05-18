package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IMailbox;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.loop.EventLoopConstants;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 每个节点都需要感知其他节点是否是 online 还是 offline。
 * <p>
 * 去中心化：需要一直感知到当前节点的 online/offline 状态。
 */
public class DecentrationNodeTimeoutCheckEventListener
        extends NodeTimeoutCheckEventListenerSupport<IMailbox<InteractivePayload>> {

    private AbstractRemotingChannel remotingChannel;
    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(DecentrationNodeTimeoutCheckEventListener.class);

    public DecentrationNodeTimeoutCheckEventListener(
            AbstractRemotingChannel remotingChannel, IRemotingManager remotingManager) {
        super(remotingManager);
        this.remotingChannel = remotingChannel;
    }

    @Override
    public boolean onEvent(ObjectEvent<IMailbox<InteractivePayload>> event,
                           int listenerIndex) {

        event.setParameter(EventLoopConstants.EVENT_LOOP_INTERVAL_PARAM,
                TimeUnit.SECONDS.toMillis(2));

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

        // 表示超时了。记录超时次数
        Long heartbeatTimeoutCount = event
                .getParameter(RemotingInteractiveConstants.HEARTBEAT_TIMEOUT_COUNT, 1l);
        if (heartbeatTimeoutCount <= 3) {
            heartbeatTimeoutCount++;
            event.setParameter(RemotingInteractiveConstants.HEARTBEAT_TIMEOUT_COUNT,
                    heartbeatTimeoutCount);
            // 进入下一轮超时检测
            return false;
        }
        event.removeParameter(RemotingInteractiveConstants.HEARTBEAT_TIMEOUT_COUNT);

        String selfIdentify = remotingManager.getChannelIdentify(
                remotingManager.getRaftConfig().getNodeInformation().identify());
        try {
            InteractivePayload responsePayload = remotingChannel
                    .requestResponse(InteractivePayload.newBuilder().setEventType(
                            RemotingEventDispatcher.REMOTING_CHECK_IS_IN_CHANNEL_REGISTRY_EVENT_TYPE)
                            .setSource(selfIdentify)
                            .setPayload(ByteString
                                    .copyFrom(selfIdentify.getBytes(CharsetUtil.UTF_8)))
                            .build());
            System.err.println(String.format("double check node is contains %s:%s",
                    selfIdentify, responsePayload.getPayload().toStringUtf8())
                    + "; channel identify :" + remotingChannel.identify());
            // is active，说明当前 {@remoting channel} 指向的节点列表中没有当前这个节点，需要进行一次 leader meet
            if (!Boolean.valueOf(responsePayload.getPayload().toStringUtf8())) {
                remotingManager.getEventLoopGroup().publish(remotingChannel,
                        AbstractRemotingManager.NODE_MEET_EVENT_TYPE);
            }
        } catch (Exception e) {

            return onException(e, remotingChannel);
        }
        // 节点会晤之后，正常情况下就应该可以收到发送过来的心跳了。因此这里不需要退出循环。
        return false;
    }

    /**
     * 去中心化实现，需要一直感知他的上线状态。
     *
     * @param e
     * @param abstractRemotingChannelbs
     * @return
     */
    public boolean onException(Exception e,
                               AbstractRemotingChannel abstractRemotingChannelbs) {

        return false;
    }
}
