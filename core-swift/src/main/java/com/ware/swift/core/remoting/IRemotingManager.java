package com.ware.swift.core.remoting;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.WareSwiftGlobalContext;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.core.remoting.channel.IRemotingChannelFactory;
import com.ware.swift.event.loop.AbstractAsyncEventLoopGroup;
import com.ware.swift.proto.InteractivePayload;

import java.util.Collection;

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
     * @param wareSwiftConfig
     * @return
     */
    boolean init(WareSwiftConfig wareSwiftConfig);

    /**
     * 开始节点选举
     */
    void startLeaderElection(WareSwiftGlobalContext wareSwiftGlobalContext);

    /**
     * @return
     */
    WareSwiftConfig getWareSwiftConfig();

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
     * @return
     */
    ITransactionModel getTransactionModel();

    /**
     * 返回当前节点感知到整个集群存活的节点数。
     * <p>
     * 注意：不包含当前节点。
     * </p>
     *
     * @return
     */
    int getActiveChannelCount();

    /**
     *
     */
    void isOnlineWithRemotingChannel(String source);

    /**
     * @param wareSwiftConfig
     */
    void setRaftConfig(WareSwiftConfig wareSwiftConfig);

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
