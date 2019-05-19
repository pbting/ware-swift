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

    /**
     *
     */
    class LeaderFollowerRemotingManager implements ILeaderFollowerRemotingManager {

        public static final ILeaderFollowerRemotingManager DEFAULT_IMPL = new LeaderFollowerRemotingManager();

        @Override
        public void processClusterNodes(AbstractRemotingManager abstractRemotingManager, String nodes) {
            String[] identifys = nodes.split("[|]");
            List<String> identifyList = Arrays.asList(identifys);
            for (Iterator<String> iterator = identifyList.iterator(); iterator.hasNext(); ) {
                String identify = iterator.next();

                if (abstractRemotingManager.getChannelIdentify(abstractRemotingManager.getRaftConfig().getNodeInformation().identify())
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
                            .newRemotingChannel(nodeInfos[1], nodeInfos[2]);
                    abstractRemotingManager.addRemotingChannel(newAddRemotingChannel);
                }
            }
        }
    }
}
