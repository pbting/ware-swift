package com.ware.swift.core.remoting;

import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public interface ILeaderFollowerRemotingManager {

    /**
     * @param abstractRemotingManager
     * @param nodes
     */
    default void processClusterNodes(AbstractRemotingManager abstractRemotingManager, String nodes) {

        // nothing to do
    }

    default void initEventListener(AbstractRemotingManager abstractRemotingManager) {

        // nothing to do
    }

    /**
     *
     */
    class LeaderFollowerRemotingManager implements ILeaderFollowerRemotingManager {

        public static final ILeaderFollowerRemotingManager DEFAULT_IMPL = new LeaderFollowerRemotingManager();

        @Override
        public void initEventListener(AbstractRemotingManager abstractRemotingManager) {
            // 1、
            abstractRemotingManager.initStartupEventListener();
            // 2、
            abstractRemotingManager.initSayHelloEventListener();
            // 3、
            abstractRemotingManager.initLeaderElectionEventListener(true);
            // 4、
            abstractRemotingManager.initSendHeartbeatEventListener();
            // 5、
            abstractRemotingManager.initTimeoutCheckEventListener();
            // 6、
            abstractRemotingManager.initBroadcastSdownEventListener();
            // 7、
            abstractRemotingManager.initBroadcastLeaderEventListener();
            // 8、
            abstractRemotingManager.initNodeMeetEventListener();
            // 9、
            abstractRemotingManager.initBroadcastNewNodeEventListener();
            //
            abstractRemotingManager.initSyncFailEventListener();
            //
            abstractRemotingManager.initEventPartitioner();
        }

        @Override
        public void processClusterNodes(AbstractRemotingManager abstractRemotingManager, String nodes) {
            abstractRemotingManager.getEventLoopGroup().getParallelQueueExecutor().executeOneTime(() -> {
                String[] identifies = nodes.split("[|]");
                List<String> identifyList = Arrays.asList(identifies);
                for (Iterator<String> iterator = identifyList.iterator(); iterator.hasNext(); ) {
                    String identify = iterator.next();

                    if (abstractRemotingManager.getChannelIdentify(abstractRemotingManager.getWareSwiftConfig().getNodeInformation().identify())
                            .equalsIgnoreCase(identify)) {
                        // skip self
                        continue;
                    }

                    if (!abstractRemotingManager.isContainsRemotingChannel(identify)) {
                        // 不包含，则建立连接。
                        String[] nodeInfos = identify.split("@");
                        if (nodeInfos.length != 3) {
                            continue;
                        }

                        AbstractRemotingChannel newAddRemotingChannel = RemotingManager
                                .getRemotingManager().getRemotingChannelFactory()
                                .newRemotingChannel(nodeInfos[1], nodeInfos[2], AbstractRemotingChannel.JoinType.NODE_MEET);
                        abstractRemotingManager.addRemotingChannel(newAddRemotingChannel);
                    }
                }
            });
        }
    }
}
