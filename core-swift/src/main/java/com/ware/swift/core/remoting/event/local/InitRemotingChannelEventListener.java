package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.WareCoreSwiftConfig;
import com.ware.swift.core.remoting.AbstractRemotingManager;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.ObjectEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 *
 */
public abstract class InitRemotingChannelEventListener
        extends AbstractLocalPipelineEventListener<WareCoreSwiftConfig> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(InitRemotingChannelEventListener.class);

    public InitRemotingChannelEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(final ObjectEvent<WareCoreSwiftConfig> event, int listenerIndex) {
        List<String> clusterNodes = event.getValue().getClusterNodes();
        final String clusterName = event.getValue().getNodeInformation().getClusterName();
        clusterNodes.forEach(clusterNode -> {
            logger.debug("has know the node for " + clusterNode);

            if (event.getValue().getNodeInformation().getAddressPort()
                    .equalsIgnoreCase(clusterNode)) {
                // skip self
                return;
            }

            // 初始化和每个节点连接
            AbstractRemotingChannel remotingChannel = RemotingManager.getRemotingManager()
                    .getRemotingChannelFactory()
                    .newRemotingChannel(clusterNode, clusterName);

            remotingManager.addRemotingChannel(remotingChannel);
            onAfterNewRemotingChannel(remotingChannel);
        });
        remotingManager.getEventLoopGroup().removeListener(this,
                AbstractRemotingManager.START_UP_EVENT_TYPE);
        return true;
    }

    public abstract void onAfterNewRemotingChannel(
            AbstractRemotingChannel remotingChannel);
}
