package com.ware.swift.core.remoting;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.event.local.DecentralizeInitRemotingChannelEventListener;
import com.ware.swift.proto.InteractivePayload;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public interface IDecentralizeRemotingManager {

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

    default StringBuilder builderRemotingChannels(String source) {

        //nothing to do
        return new StringBuilder();
    }

    default long generatorTerm(String identify) {
        char[] nodeCharArray = identify.toCharArray();
        long term = 0;
        for (char ch : nodeCharArray) {
            term += ch;
        }
        return term;
    }

    default void initEventListener(AbstractRemotingManager remotingManager) {
        // nothing to do
    }

    /**
     *
     */
    class DecentralizeRemotingManager implements IDecentralizeRemotingManager {

        public static final IDecentralizeRemotingManager DEFAULT_IMPL = new DecentralizeRemotingManager();

        @Override
        public void initEventListener(AbstractRemotingManager remotingManager) {
            // 1、
            remotingManager.initStartupEventListener();
            // 2、
            remotingManager.initSayHelloEventListener();
            // 3、
            remotingManager.initSendHeartbeatEventListener();
            // 4、
            remotingManager.initBroadcastSdownEventListener();
            // 5、
            remotingManager.initNodeMeetEventListener();
            // 6、
            remotingManager.initBroadcastNewNodeEventListener();
            // 7、
            remotingManager.initSyncFailEventListener();
            //
            remotingManager.initEventPartitioner();
        }

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
                    .getChannelIdentify(RemotingManager.getRemotingManager().getWareSwiftConfig()
                            .getNodeInformation().identify()));

            return remotingChannelsSB;
        }

        /**
         * @param value
         * @param nodesHeartbeatMailboxRegistry
         */
        @Override
        public void producer(InteractivePayload value, Map<String, IMailbox> nodesHeartbeatMailboxRegistry) {
            IMailbox source = nodesHeartbeatMailboxRegistry.get(value.getSource());
            if (source == null) {
                // 新节点的加入。
                NodeInformation nodeInformation =
                        (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER.decodeResult(value.getPayload().toByteArray(), NodeInformation.class);
                IRemotingManager remotingManager = RemotingManager.getRemotingManager();
                AbstractRemotingChannel remotingChannel = AbstractRemotingChannel.addNewNode(nodeInformation.getAddressPort(), nodeInformation.getClusterName(), AbstractRemotingChannel.JoinType.NODE_MEET);
                new DecentralizeInitRemotingChannelEventListener(remotingManager).onAfterNewRemotingChannel(remotingChannel);
                return;
            }

            source.producer(value);
        }

        /**
         * 去中心化的架构下，新加入的节点，会和每个节点进行一次会晤。
         *
         * @param abstractRemotingManager
         * @param nodes
         */
        @Override
        public void processClusterNodes(AbstractRemotingManager abstractRemotingManager, String nodes) {
            // 异步建立连接，以防阻塞上有代码的正确执行
            abstractRemotingManager.getEventLoopGroup().getParallelQueueExecutor().executeOneTime(() -> {
                String[] ptpNodesArry = nodes.split("[|]");
                for (String ptpNode : ptpNodesArry) {
                    // role@ip:port@cluster_name
                    String[] infos = ptpNode.split("[@]");
                    if (infos.length != 3) {
                        continue;
                    }

                    if (!RemotingInteractiveConstants.PTP_ROLE_PREFIX
                            .equalsIgnoreCase(infos[0])) {
                        continue;
                    }

                    if (abstractRemotingManager.getChannelIdentify(abstractRemotingManager.getWareSwiftConfig().getNodeInformation().identify())
                            .equalsIgnoreCase(ptpNode)) {
                        // skip self
                        continue;
                    }

                    if (abstractRemotingManager.isContainsRemotingChannel(ptpNode)) {
                        continue;
                    }
                    // 不包含，则建立起连接
                    AbstractRemotingChannel abstractRemotingChannel = abstractRemotingManager.getRemotingChannelFactory()
                            .newRemotingChannel(infos[1], infos[2], AbstractRemotingChannel.JoinType.NODE_MEET);
                    if (abstractRemotingManager.addRemotingChannel(abstractRemotingChannel)) {
                        new DecentralizeInitRemotingChannelEventListener(abstractRemotingManager)
                                .onAfterNewRemotingChannel(abstractRemotingChannel);
                    } else {
                        abstractRemotingChannel.close();
                    }
                }
            });
        }
    }
}
