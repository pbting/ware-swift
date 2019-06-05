package com.ware.swift.core.remoting.event.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.remoting.*;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;

/**
 * 运行态下的一个数据同步处理入口。处理 Leader-Follower 架构下的数据同步
 */
public class RemotingRuntimeDataSyncEventListener extends AbstractRemotingPipelineEventListener {
    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(RemotingRuntimeDataSyncEventListener.class);

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        InteractivePayload interactivePayload = event.getValue().getInteractivePayload();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        WareSwiftConfig wareSwiftConfig = RemotingManager.getRemotingManager().getWareSwiftConfig();
        builder.setSource(RemotingManager.getRemotingManager()
                .getChannelIdentify(wareSwiftConfig.getNodeInformation().identify()));
        // 添加到正在同步 payload 的消息列表中。
        String className = interactivePayload.getHeadersMap()
                .get(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS);
        try {
            Class clazz = Class.forName(className);
            RemotingDomainSupport remotingDomain = (RemotingDomainSupport) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(interactivePayload.getPayload().toByteArray(), clazz);
            RemotingManager.getRemotingManager().getCapabilityModel().onInboundDataSet(interactivePayload.getSource(),
                    remotingDomain, new HashMap(interactivePayload.getHeadersMap()));
            builder.putHeaders(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE,
                    String.valueOf(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE_SUCCESS));
            builder.setPayload(ByteString.copyFrom(ClusterDataSyncManager
                    .formatSuccessReasonPhrase(className).getBytes(CharsetUtil.UTF_8)));
        } catch (Exception e) {
            builder.putHeaders(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE,
                    String.valueOf(ClusterDataSyncManager.DATA_SYNC_STATUS_CODE_ERROR));
            builder.setPayload(ByteString.copyFrom(ClusterDataSyncManager
                    .formatErrorReasonPhrase(e.getClass().getName(), e.getMessage())
                    .getBytes(CharsetUtil.UTF_8)));
            log.error("remoting exchange the class cause an exception,", e);
        } finally {
            event.getValue().sendPayload(builder.build());
        }
        return true;
    }
}
