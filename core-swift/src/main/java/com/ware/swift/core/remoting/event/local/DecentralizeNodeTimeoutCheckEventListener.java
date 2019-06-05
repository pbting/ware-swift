package com.ware.swift.core.remoting.event.local;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.AbstractRemotingManager;
import com.ware.swift.core.remoting.IMailbox;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.remoting.RemotingEventDispatcher;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.loop.EventLoopConstants;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

/**
 * 每个节点都需要感知其他节点是否是 online 还是 offline。
 * <p>
 * 去中心化：需要一直感知到当前节点的 online/offline 状态。
 * </p>
 */
public class DecentralizeNodeTimeoutCheckEventListener
        extends NodeTimeoutCheckEventListenerSupport<IMailbox<InteractivePayload>> {

    private AbstractRemotingChannel remotingChannel;
    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(DecentralizeNodeTimeoutCheckEventListener.class);

    private static final Hashtable<String, NodeInformation> peerNodeInformationRepository = new Hashtable<>();

    public DecentralizeNodeTimeoutCheckEventListener(
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
                remotingManager.getWareSwiftConfig().getNodeInformation().identify());
        try {
            InteractivePayload responsePayload = remotingChannel
                    .requestResponse(InteractivePayload.newBuilder().setEventType(
                            RemotingEventDispatcher.REMOTING_CHECK_IS_IN_CHANNEL_REGISTRY_EVENT_TYPE)
                            .setSource(selfIdentify)
                            .setPayload(ByteString
                                    .copyFrom(selfIdentify.getBytes(CharsetUtil.UTF_8)))
                            .build());
            logger.info(String.format("double check node is contains %s:%s",
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

    @Override
    public void processHeartbeatPayload(InteractivePayload heartbeatPayload) {
        byte[] nodeInfoBytes = heartbeatPayload.getPayload().toByteArray();
        NodeInformation nodeInformation =
                (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER.decodeResult(nodeInfoBytes, NodeInformation.class);
        peerNodeInformationRepository.put(nodeInformation.identify(), nodeInformation);
        super.processHeartbeatPayload(heartbeatPayload);
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
