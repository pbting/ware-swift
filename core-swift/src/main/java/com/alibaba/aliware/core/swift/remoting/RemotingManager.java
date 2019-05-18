package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.WareCoreSwiftConfig;
import com.alibaba.aliware.core.swift.WareCoreSwiftGlobalContext;
import com.alibaba.aliware.core.swift.WareCoreSwiftPluginLoader;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.channel.IRemotingChannelFactory;
import com.alibaba.aliware.grpc.swift.event.swift.loop.AbstractAsyncEventLoopGroup;
import com.alibaba.aliware.swift.proto.InteractivePayload;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 *
 */
public class RemotingManager implements IRemotingManager {

    private static final String LEADER_FOLLOWER_RSOCKET_REMOTING_MANAGER =
            "com.alibaba.aliware.rsocket.swift.LeaderFollowerRSocketRemotingManager";

    private static final String LEADER_FOLLOWER_GRPC_REMOTING_MANAGER = "com.alibaba.aliware.grpc.swift.LeaderFollowerGrpcRemotingManager";

    private static final int LEADER_FOLLOWER_REMOTING_MANAGER_TYPE = 1 << 1;

    private static final int DECENTRATION_REMOTING_MANAGER_TYPE = 1 << 2;

    public static int REMOTING_MANAGER_TYPE = 1;

    static IRemotingManager remotingManagerWrapper;

    private static final IRemotingManager REMOTING_MANAGER = new RemotingManager();

    static {
        try {
            // 1、初始化 remoting manager
            Set<IRemotingManager> remotingManagers = WareCoreSwiftPluginLoader
                    .load(IRemotingManager.class, RemotingManager.class.getClassLoader());
            if (remotingManagers == null || remotingManagers.isEmpty()) {
                Class rsocketRemotingManager = Class.forName(LEADER_FOLLOWER_RSOCKET_REMOTING_MANAGER);
                remotingManagerWrapper = (IRemotingManager) rsocketRemotingManager.newInstance();
            } else {
                remotingManagerWrapper = remotingManagers.iterator().next();
            }

            if (remotingManagerWrapper instanceof ILeaderFollowerRemotingManager) {
                REMOTING_MANAGER_TYPE |= LEADER_FOLLOWER_REMOTING_MANAGER_TYPE;
            }

            if (remotingManagerWrapper instanceof IDecentrationRemotingManager) {
                REMOTING_MANAGER_TYPE |= DECENTRATION_REMOTING_MANAGER_TYPE;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isLeaderFollowerRemotingManager() {

        return (REMOTING_MANAGER_TYPE
                & LEADER_FOLLOWER_REMOTING_MANAGER_TYPE) == LEADER_FOLLOWER_REMOTING_MANAGER_TYPE;
    }

    public static boolean isDecenterationRemotingManager() {
        return (REMOTING_MANAGER_TYPE
                & DECENTRATION_REMOTING_MANAGER_TYPE) == DECENTRATION_REMOTING_MANAGER_TYPE;
    }

    public static IRemotingManager getRemotingManager() {
        return REMOTING_MANAGER;
    }

    @Override
    public boolean init(WareCoreSwiftConfig raftConfig) {
        return remotingManagerWrapper.init(raftConfig);
    }

    @Override
    public void startLeaderElection(WareCoreSwiftGlobalContext raftGlobaleContext) {
        remotingManagerWrapper.startLeaderElection(raftGlobaleContext);
    }

    @Override
    public WareCoreSwiftConfig getRaftConfig() {
        return remotingManagerWrapper.getRaftConfig();
    }

    @Override
    public void sendHeartbeats(AbstractRemotingChannel remotingChannel) {
        remotingManagerWrapper.sendHeartbeats(remotingChannel);
    }

    @Override
    public void startLeaderHeartbeatTimeoutCheck() {
        remotingManagerWrapper.startLeaderHeartbeatTimeoutCheck();
    }

    @Override
    public void producer(InteractivePayload value) {
        remotingManagerWrapper.producer(value);
    }

    @Override
    public InteractivePayload consumer(int timeout, TimeUnit timeUnit) {
        return remotingManagerWrapper.consumer(timeout, timeUnit);
    }

    @Override
    public void broadcastSdown(NodeInformation sdownNode) {

        remotingManagerWrapper.broadcastSdown(sdownNode);
    }

    @Override
    public void reElectionForLeader() {
        remotingManagerWrapper.reElectionForLeader();
    }

    public IRemotingChannelFactory getRemotingChannelFactory() {
        return remotingManagerWrapper.getRemotingChannelFactory();
    }

    @Override
    public boolean addRemotingChannel(AbstractRemotingChannel remotingChannel) {
        return remotingManagerWrapper.addRemotingChannel(remotingChannel);
    }

    @Override
    public Collection<AbstractRemotingChannel> getRemotingChannels() {
        return remotingManagerWrapper.getRemotingChannels();
    }

    @Override
    public void broadcastNewNode(NodeInformation nodeInformation) {

        remotingManagerWrapper.broadcastNewNode(nodeInformation);
    }

    @Override
    public boolean isContainsRemotingChannel(String identify) {
        return remotingManagerWrapper.isContainsRemotingChannel(identify);
    }

    @Override
    public int getActiveChannelCount() {
        return remotingManagerWrapper.getActiveChannelCount();
    }

    @Override
    public void isOnlineWithRemotingChannel(String source) {
        remotingManagerWrapper.isOnlineWithRemotingChannel(source);
    }

    @Override
    public void setRaftConfig(WareCoreSwiftConfig raftConfig) {
        remotingManagerWrapper.setRaftConfig(raftConfig);
    }

    @Override
    public AbstractAsyncEventLoopGroup getEventLoopGroup() {
        return remotingManagerWrapper.getEventLoopGroup();
    }

    @Override
    public int getSendIdentifyCount() {
        return remotingManagerWrapper.getSendIdentifyCount();
    }

    @Override
    public void increSendIdentifyCount() {
        remotingManagerWrapper.increSendIdentifyCount();
    }

    @Override
    public AbstractRemotingChannel getRemotingChannel(String identify) {
        return remotingManagerWrapper.getRemotingChannel(identify);
    }

    @Override
    public <T> void setServer(T server) {
        remotingManagerWrapper.setServer(server);
    }

    @Override
    public String getChannelIdentify(String key) {
        return remotingManagerWrapper.getChannelIdentify(key);
    }

    @Override
    public void addHeartbeatTimeoutCheckMailbox(IMailbox<InteractivePayload> mailbox) {
        remotingManagerWrapper.addHeartbeatTimeoutCheckMailbox(mailbox);
    }

    @Override
    public IMailbox<InteractivePayload> getMailbox() {
        return remotingManagerWrapper.getMailbox();
    }

    @Override
    public void processClusterNodes(String nodes) {
        remotingManagerWrapper.processClusterNodes(nodes);
    }

    @Override
    public ICapabilityModel getCapabilityModel() {
        return remotingManagerWrapper.getCapabilityModel();
    }

    @Override
    public void sendHeartbeatsIfExist(String sourceIdentifyChannel) {

        remotingManagerWrapper.sendHeartbeatsIfExist(sourceIdentifyChannel);
    }

    @Override
    public IClientInteractivePayloadHandler getClientInteractivePayloadHandler() {

        return remotingManagerWrapper.getClientInteractivePayloadHandler();
    }

    @Override
    public int addSyncingRemotingDoamin(RemotingDomainWrapper remotingDoaminWrapper) {

        return remotingManagerWrapper.addSyncingRemotingDoamin(remotingDoaminWrapper);
    }

    @Override
    public Collection<RemotingDomain> getCommittedRemotingDomains(
            Collection<String> committedIds, AbstractRemotingChannel remotingChannel) {
        return remotingManagerWrapper.getCommittedRemotingDomains(committedIds,
                remotingChannel);
    }

    @Override
    public void removeSyncingRemotingDoamin(RemotingDomainWrapper remotingDoaminWrapper) {

        remotingManagerWrapper.removeSyncingRemotingDoamin(remotingDoaminWrapper);
    }

    @Override
    public String prepareCommittedRemotingDomains(String channelSource, long term) {
        return remotingManagerWrapper.prepareCommittedRemotingDomains(channelSource,
                term);
    }

    @Override
    public void clearSyncingRemotingDomainIds() {

        remotingManagerWrapper.clearSyncingRemotingDomainIds();
    }

    @Override
    public Collection<RemotingDomain> committedSyncingDomains(long term) {

        return remotingManagerWrapper.committedSyncingDomains(term);
    }

    @Override
    public <V> void putIfAbsentStreamReplayProcessor(String streamTopicIdentify,
                                                     V streamReplayProcessor) {
        remotingManagerWrapper.putIfAbsentStreamReplayProcessor(streamTopicIdentify,
                streamReplayProcessor);
    }

    @Override
    public void visiterSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor) {
        remotingManagerWrapper.visiterSyncingRemotingDomain(visitor);
    }

    @Override
    public int getSyncingRemotingDomainSize() {

        return remotingManagerWrapper.getSyncingRemotingDomainSize();
    }

    @Override
    public void implyVoteFor(String implyVoteFor) {

        remotingManagerWrapper.implyVoteFor(implyVoteFor);
    }

    @Override
    public String getImpliedVoteFor() {
        return remotingManagerWrapper.getImpliedVoteFor();
    }
}
