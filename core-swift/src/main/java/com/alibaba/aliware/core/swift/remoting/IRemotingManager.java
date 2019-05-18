package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.WareCoreSwiftConfig;
import com.alibaba.aliware.core.swift.WareCoreSwiftGlobalContext;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.channel.IRemotingChannelFactory;
import com.alibaba.aliware.grpc.swift.event.swift.loop.AbstractAsyncEventLoopGroup;
import com.alibaba.aliware.swift.proto.InteractivePayload;

import java.util.Collection;
import java.util.function.Consumer;

/**
 *
 */
public interface IRemotingManager extends IMailbox<InteractivePayload> {

    /**
     * @return
     */
    int getSendIdentifyCount();

    /**
     *
     */
    void increSendIdentifyCount();

    /**
     * @param raftConfig
     * @return
     */
    boolean init(WareCoreSwiftConfig raftConfig);

    /**
     * 开始节点选举
     */
    void startLeaderElection(WareCoreSwiftGlobalContext raftGlobaleContext);

    /**
     * @return
     */
    WareCoreSwiftConfig getRaftConfig();

    /**
     * @param remotingChannel
     */
    void sendHeartbeats(AbstractRemotingChannel remotingChannel);

    /**
     * @param sourceIdentifyChannel
     */
    void sendHeartbeatsIfExist(String sourceIdentifyChannel);

    /**
     * 开启 接收 leader 心跳超时检测
     */
    void startLeaderHeartbeatTimeoutCheck();

    /**
     * 广播给其他 Follower 节点，通知
     */
    void broadcastSdown(NodeInformation sdownNode);

    /**
     * 广播新加入的节点
     */
    void broadcastNewNode(NodeInformation nodeInformation);

    /**
     * 检测是否需要重新一次 Leader 选举
     */
    void reElectionForLeader();

    /**
     * @return
     */
    IRemotingChannelFactory getRemotingChannelFactory();

    /**
     * 运行在运行的过程中新添加一个 remoting channel。
     * <p>
     * 这种情况当扩容一个节点的时候会发生。
     *
     * @param remotingChannel
     */
    boolean addRemotingChannel(AbstractRemotingChannel remotingChannel);

    /**
     * @param identify
     * @return
     */
    AbstractRemotingChannel getRemotingChannel(String identify);

    /**
     * @param key
     * @return
     */
    String getChannelIdentify(String key);

    /**
     * 可以获取当前所有已经建立连接的 remoting channel
     *
     * @return
     */
    Collection<AbstractRemotingChannel> getRemotingChannels();

    /**
     * 判断当前 leader 节点是否已经和 Follower 节点建立连接
     *
     * @return
     */
    boolean isContainsRemotingChannel(String identify);

    /**
     * @param remotingDoaminWrapper
     * @return
     */
    int addSyncingRemotingDoamin(RemotingDomainWrapper remotingDoaminWrapper);

    /**
     * 访问 正在同步中的 remoting domain
     */
    void visiterSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor);

    /**
     * @param remotingDoaminWrapper
     */
    void removeSyncingRemotingDoamin(RemotingDomainWrapper remotingDoaminWrapper);

    /**
     * 对 committed 计数增一，同时收集已经 committed 成功的 remoting domain,和 out of syncing water marker
     *
     * @param remotingChannel
     */
    Collection<RemotingDomain> getCommittedRemotingDomains(
            Collection<String> committedIds, AbstractRemotingChannel remotingChannel);

    /**
     * 获取正在同步g过程中的的 id。每个id 之间用逗号隔开。
     *
     * @return
     */
    String prepareCommittedRemotingDomains(String channelSource, long term);

    /**
     * @return
     */
    int getSyncingRemotingDomainSize();

    /**
     * 这是一个选择性实现的接口。
     */
    void clearSyncingRemotingDomainIds();

    /**
     * @param term 当前收到 leader 发送过来的 term。需要考虑网络分区后的重建，处理落后的 term 同步数据
     * @return
     */
    Collection<RemotingDomain> committedSyncingDomains(long term);

    /**
     * @return
     */
    int getActiveChannelCount();

    /**
     *
     */
    void isOnlineWithRemotingChannel(String source);

    /**
     * @param raftConfig
     */
    void setRaftConfig(WareCoreSwiftConfig raftConfig);

    /**
     * @return
     */
    AbstractAsyncEventLoopGroup getEventLoopGroup();

    /**
     * @param server
     * @param <T>
     */
    <T> void setServer(T server);

    /**
     * @return
     */
    IMailbox<InteractivePayload> getMailbox();

    /**
     * 添加心跳超时检测的 mailbox。
     *
     * @param mailbox
     */
    void addHeartbeatTimeoutCheckMailbox(IMailbox<InteractivePayload> mailbox);

    /**
     * 收到集群节点列表后的处理方式.
     * <p>
     * 每个节点以 | 分隔
     */
    void processClusterNodes(String nodes);

    /**
     * @return
     */
    ICapabilityModel getCapabilityModel();

    /**
     * @return
     */
    IClientInteractivePayloadHandler getClientInteractivePayloadHandler();

    /**
     * @param streamTopicIdentify
     * @param streamReplayProcessor
     * @param <V>
     */
    <V> void putIfAbsentStreamReplayProcessor(String streamTopicIdentify,
                                              V streamReplayProcessor);

    /**
     * @return
     */
    void implyVoteFor(String implyVoteFor);

    /**
     * @return
     */
    String getImpliedVoteFor();
}
