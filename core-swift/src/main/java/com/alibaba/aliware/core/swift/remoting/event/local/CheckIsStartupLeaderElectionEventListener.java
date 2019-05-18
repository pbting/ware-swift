package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.WareCoreSwiftGlobalContext;
import com.alibaba.aliware.core.swift.remoting.AbstractRemotingManager;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.event.remoting.RemotingEventDispatcher;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.loop.EventLoopConstants;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class CheckIsStartupLeaderElectionEventListener
        extends AbstractLocalPipelineEventListener<WareCoreSwiftGlobalContext> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(CheckIsStartupLeaderElectionEventListener.class);

    // 是否需要等到 all ready。
    private boolean isWaitAllReady;

    /**
     * @param isWaitAllReady  在运行过程中触发的 Leader 选举是不需要等待所有的节点是否已经 ready 的状态。因此这个时候 这个值为
     *                        false。集群刚启动的时候，改值为 true，需要等待所有的节点已经启动 ready 好，才可以进行 leader 的选举操作。
     * @param remotingManager
     */
    public CheckIsStartupLeaderElectionEventListener(boolean isWaitAllReady,
                                                     IRemotingManager remotingManager) {
        super(remotingManager);
        this.isWaitAllReady = isWaitAllReady;
    }

    @Override
    public boolean onEvent(ObjectEvent<WareCoreSwiftGlobalContext> event, int listenerIndex) {
        // 中断后面的 listener 执行。已经是最后一个 listener 执行了，因此此时返回的 true/false 会生效。
        event.setInterruptor(true);
        if (remotingManager.getRaftConfig() == null) {
            // wait next event loop
            return false;
        }

        // 这个逻辑主要处理在运行的过程中新增加一个节点在 say hello 之后会接受到当前集群的 Leader 信息。
        if (remotingManager.getRaftConfig().getLeader() != null) {
            logger.debug(
                    "has get leader by sayHello,so will not start to leader election and start to heartbeat timeout check.");
            // 当前集群中已经有 leader 了，直接进入心跳检测超时即可。
            RemotingManager.getRemotingManager().startLeaderHeartbeatTimeoutCheck();
            return true;
        }
        // 集群第一次初始化的时候，需要等 All Ready。运行过程中 Leader 挂了触发新的选举就需要等待了。
        // 因此要加个判断是运行态还是初始状态触发的
        boolean isStart = false;
        if (isWaitAllReady && remotingManager.getSendIdentifyCount() == remotingManager
                .getRaftConfig().getClusterNodes().size() - 1) {
            logger.debug("start to leader election");
            isStart = true;
        }

        if (!isWaitAllReady) {
            processImplyVoteFor();
            isStart = true;
        }

        if (isStart) {
            remotingManager.getEventLoopGroup().removeListener(this,
                    AbstractRemotingManager.LEADER_ELECTION_EVENT_TYPE);
            long candidateTimeOut = 150 + new Random().nextInt(200);
            event.setParameter(EventLoopConstants.EVENT_LOOP_INTERVAL_PARAM,
                    candidateTimeOut);
        }
        logger.debug("wait all of identify payload send success");
        // 返回 false,标识 event loop 还没有结束，等待下一次的 event loop。
        return false;
    }

    /**
     *
     */
    private void processImplyVoteFor() {

        final AtomicReference<AbstractRemotingChannel> maxCommittedChannel = new AtomicReference();
        final AtomicBoolean isGreater = new AtomicBoolean(false);
        RemotingManager.getRemotingManager().getRemotingChannels()
                .forEach(remotingChannel -> {

                    if (remotingChannel.isOpenOnlineCheck()) {

                        return;
                    }

                    InteractivePayload.Builder builder = InteractivePayload.newBuilder();
                    String channelSource = RemotingManager.getRemotingManager()
                            .getChannelIdentify(RemotingManager.getRemotingManager()
                                    .getRaftConfig().getNodeInformation().identify());
                    builder.setSource(channelSource);
                    builder.setSink(remotingChannel.identify());
                    builder.setEventType(
                            RemotingEventDispatcher.REMOTING_GET_COMMITTED_EVENT_TYPE);
                    InteractivePayload response = remotingChannel
                            .requestResponse(builder.build());
                    logger.info(remotingChannel.identify()
                            + "; retrieve the committed count is "
                            + response.getPayload().toStringUtf8());
                    long committed = Long.valueOf(response.getPayload().toStringUtf8());
                    remotingChannel.setCommittedCount(committed);

                    if (maxCommittedChannel.get() == null) {
                        maxCommittedChannel.set(remotingChannel);
                        return;
                    }

                    if (committed > maxCommittedChannel.get().getCommittedCount()) {
                        maxCommittedChannel.set(remotingChannel);
                        isGreater.set(true);
                    }

                });

        if (isGreater.get()) {
            logger.info("will imply vote for " + maxCommittedChannel.get().identify());
            RemotingManager.getRemotingManager()
                    .implyVoteFor(maxCommittedChannel.get().identify());
        }
    }
}
