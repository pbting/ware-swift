package com.ware.swift.core.remoting;

import com.google.protobuf.ByteString;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Set;

/**
 * 默认的客户端交互式请求处理的入口。业务方基于此框架来开发出服务自己业务语义的中间件时，处理的入口就在这里。
 * <p>
 * 通常的处理方式有：
 * <p>
 * 1. 判断当前的请求是查询的，还是 create/update/delete
 * <p>
 * 2.
 * 如果确定数据要同步，则调用{@link ICapabilityModel#onOutboundDataSet(RemotingDomainSupport, OutboundCallback)}
 * (InteractivePayload)}
 * <p>
 * 需要同步的数据默认情况下会在下一次发送心跳的时候给同步其他节点。这种情况下通常是在 AP 模型下需要。
 * <p>
 * 3.
 * 如果无需等待下一次发送心跳的时候就需要将数据立马同步到其他节点，这个时候可以调用{@link ICapabilityModel#onOutboundDataSet(RemotingDomainSupport, OutboundCallback)}
 * (InteractivePayload, Callable)}
 */
public class DefaultClientInteractivePayloadHandler
        implements IClientInteractivePayloadHandler {

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(DefaultClientInteractivePayloadHandler.class);

    /**
     *
     */
    public static final String CLIENT_SINK_GET_LEADER = "getLeader";

    /**
     *
     */
    public static final String CLIENT_SINK_DATA_SYNC = "dataSync";

    /**
     * 客户端发送请求的处理入口。
     * <p>
     * 这里需要反序列指定的对象，会根据类似于处理 http 请求一样不通的 url，有不同的请求参数。
     * <p>
     * 可以通过 InteractivePayload#sink
     * <p>
     * 这个字段来描述类似于 http 请求的 URI，然后会有不同的类似于 Controller 的处理器，再根据自己的需要反序列化指定的对象类型。
     *
     * @param interactive
     */
    @Override
    public void handler(final IInteractive interactive) {
        InteractivePayload interactivePayload = interactive.getInteractivePayload();
        String sink = interactivePayload.getSink();
        if (CLIENT_SINK_GET_LEADER.equals(sink)) {
            RemotingManager remotingManager = (RemotingManager) RemotingManager
                    .getRemotingManager();
            if (remotingManager.isLeaderFollowerRemotingManager()) {
                NodeInformation leader = RemotingManager.getRemotingManager()
                        .getWareSwiftConfig().getLeader();
                processGetLeader(leader, interactive);
            } else if (remotingManager.isDecenterationRemotingManager()) {
                processGetLeader(RemotingManager.getRemotingManager().getWareSwiftConfig()
                        .getNodeInformation(), interactive);
            }
        } else if (CLIENT_SINK_DATA_SYNC.equals(sink)) {

            processDataSync(interactive, interactivePayload);
        } else {

            processCustomSink(sink, interactive);
        }
    }

    private void processGetLeader(NodeInformation nodeInformation,
                                  IInteractive interactive) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setPayload(
                ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                        .encodingResult(nodeInformation)));
        interactive.sendPayload(builder.build());
    }

    private void processDataSync(IInteractive interactive,
                                 InteractivePayload interactivePayload) {
        try {
            String clazzName = interactivePayload.getHeadersMap()
                    .get(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS);
            Class clazz = Class.forName(clazzName);
            RemotingDomainSupport remotingDomain = (RemotingDomainSupport) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                    .decodeResult(interactivePayload.getPayload().toByteArray(), clazz);

            final OutboundCallback outboundCallback = new OutboundCallback() {
                @Override
                public void onSyncingSuccess(Set<String> channelIdentifies) {
                    InteractivePayload.Builder builder = InteractivePayload.newBuilder();
                    builder.setPayload(ByteString.copyFrom(
                            channelIdentifies.toString().getBytes(CharsetUtil.UTF_8)));
                    interactive.sendPayload(builder.build());
                }

                @Override
                public void onSyncingFailed(Set<String> successChannelIdentifies,
                                            Set<String> failedChannelIdentifies) {
                    String re = successChannelIdentifies.toString() + ";"
                            + failedChannelIdentifies.toString();
                    log.info(successChannelIdentifies.toArray() + ";"
                            + failedChannelIdentifies.toArray());
                    InteractivePayload.Builder builder = InteractivePayload.newBuilder();
                    builder.setPayload(
                            ByteString.copyFrom(re.getBytes(CharsetUtil.UTF_8)));
                    interactive.sendPayload(builder.build());
                }

                @Override
                public void onMinRequestRequiredAcks(int requestRequiredAcks,
                                                     int realActiveChannels) {
                    InteractivePayload.Builder builder = InteractivePayload.newBuilder();
                    builder.setPayload(ByteString
                            .copyFrom((requestRequiredAcks + "=" + realActiveChannels)
                                    .getBytes(CharsetUtil.UTF_8)));
                    interactive.sendPayload(builder.build());
                }
            };

            RemotingManager.getRemotingManager().getEventLoopGroup()
                    .getParallelQueueExecutor().execute(interactivePayload.getSink(),
                    () -> RemotingManager.getRemotingManager()
                            .getCapabilityModel()
                            .onOutboundDataSet(remotingDomain, outboundCallback));
        } catch (ClassNotFoundException e) {
            log.error("data sync cause an exception.", e);
        }
    }

    /**
     * 当从客户端接收到的消息，默认处理没有到具体的 sink 类型。就会触发这里的逻辑。
     */
    public void processCustomSink(final String sink, final IInteractive interactive) {

        // nothing to do
    }
}
