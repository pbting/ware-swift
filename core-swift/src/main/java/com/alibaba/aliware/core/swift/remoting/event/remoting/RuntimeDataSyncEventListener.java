package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.WareCoreSwiftConfig;
import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingDomain;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 运行态下的一个数据同步处理入口。处理 Leader-Follower 架构下的数据同步
 */
public class RuntimeDataSyncEventListener extends AbstractRemotingPipelineEventListener {
    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(RuntimeDataSyncEventListener.class);

    private static final AtomicLong inboundDataSetSize = new AtomicLong();

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        InteractivePayload interactivePayload = event.getValue().getInteractivePayload();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        WareCoreSwiftConfig raftConfig = RemotingManager.getRemotingManager().getRaftConfig();
        builder.setSource(RemotingManager.getRemotingManager()
                .getChannelIdentify(raftConfig.getNodeInformation().identify()));
        // 添加到正在同步 payload 的消息列表中。
        String className = interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS);
        try {
            Class clazz = Class.forName(className);
            RemotingDomain remotingDomain = (RemotingDomain) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(interactivePayload.getPayload().toByteArray(), clazz);
            RemotingManager.getRemotingManager().getCapabilityModel().onInboundDataSet(
                    remotingDomain, new HashMap(interactivePayload.getHeadersMap()));
            builder.putHeaders(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE,
                    String.valueOf(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE_SUCCESS));
            builder.setPayload(ByteString.copyFrom(ClusterDataSyncManager
                    .formatSuccessResonPhrase(className).getBytes(CharsetUtil.UTF_8)));
        } catch (Exception e) {
            builder.putHeaders(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE,
                    String.valueOf(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE_ERROR));
            builder.setPayload(ByteString.copyFrom(ClusterDataSyncManager
                    .formatErrorResonPhrase(e.getClass().getName(), e.getMessage())
                    .getBytes(CharsetUtil.UTF_8)));
            log.error("remoting exchange the class cause an exception,", e);
        } finally {
            event.getValue().sendPayload(builder.build());
        }
        return true;
    }
}
