package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.event.local.DecentrationInitRemotingChannelEventListener;
import com.alibaba.aliware.swift.proto.InteractivePayload;

import java.util.Map;

/**
 *
 */
public interface IDecentrationRemotingManager {

    /**
     * @param value
     * @param nodesHeartbeatMailboxRegistry
     */
    default void producer(InteractivePayload value, Map<String, IMailbox> nodesHeartbeatMailboxRegistry) {

        // nothing to do
    }

    /**
     * @param abstractRemotingManager
     * @param nodes
     */
    default void processClusterNodes(AbstractRemotingManager abstractRemotingManager, String nodes) {

        // nothing to do
    }

    /**
     *
     */
    class DecentrationRemotingManager implements IDecentrationRemotingManager {

        public static final IDecentrationRemotingManager DEFAULT_IMPL = new DecentrationRemotingManager();

        /**
         * @param value
         * @param nodesHeartbeatMailboxRegistry
         */
        @Override
        public void producer(InteractivePayload value, Map<String, IMailbox> nodesHeartbeatMailboxRegistry) {
            IMailbox source = nodesHeartbeatMailboxRegistry.get(value.getSource());
            if (source == null) {
                return;
            }

            source.producer(value);
        }

        /**
         * @param abstractRemotingManager
         * @param nodes
         */
        @Override
        public void processClusterNodes(AbstractRemotingManager abstractRemotingManager, String nodes) {
            System.err.println("去中心化在 node meet 之后收到的 nodes: " + nodes);
            String[] ptpNodesArry = nodes.split("[|]");
            for (String ptpNode : ptpNodesArry) {
                // role@ip:port@cluster_name
                String[] infos = ptpNode.split("[@]");
                if (infos.length != 3) {
                    continue;
                }

                if (!RemotingInteractiveConstants.PTP_ROLE_PRIFIX
                        .equalsIgnoreCase(infos[0])) {
                    continue;
                }

                if (abstractRemotingManager.getChannelIdentify(abstractRemotingManager.getRaftConfig().getNodeInformation().identify())
                        .equalsIgnoreCase(ptpNode)) {
                    // skip self
                    continue;
                }

                if (abstractRemotingManager.isContainsRemotingChannel(ptpNode)) {
                    System.err.println("去中心话 已经包含该节点:" + ptpNode);
                    continue;
                }
                // 不包含，则建立起连接
                AbstractRemotingChannel abstractRemotingChannel = abstractRemotingManager.getRemotingChannelFactory()
                        .newRemotingChannel(infos[1], infos[2]);
                System.err.println("---->去中心化感知到节点:" + ptpNode);
                if (abstractRemotingManager.addRemotingChannel(abstractRemotingChannel)) {
                    new DecentrationInitRemotingChannelEventListener(abstractRemotingManager)
                            .onAfterNewRemotingChannel(abstractRemotingChannel);
                }
            }
        }
    }
}
