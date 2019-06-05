/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ware.swift.hot.data;

import com.google.protobuf.ByteString;
import com.ware.swift.core.WareSwiftDeveloperManager;
import com.ware.swift.core.remoting.*;
import com.ware.swift.event.ICallbackHook;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author pbting
 * @date 2019-05-19 10:49 AM
 */
public class HotDataClientRequestHandler extends DefaultClientInteractivePayloadHandler {

    private static final Logger log = Logger.getLogger(HotDataRemotingDomain.class.getCanonicalName());
    private static final Integer HOT_DATA_TOPIC_HEAD_KEY = 1000;
    private static final Integer HOT_DATA_KEY_HEAD_KEY = 1001;
    private final ConcurrentHashMap<String, IInteractive> subscriberInteractiveRegistry = new ConcurrentHashMap<>();
    private HashMap<String, Method> sinkOperatorMethodMapping = new HashMap<>();

    public static final String SINK_ADD_REMOTING_DOMAIN_OP = "add/remoting/domain.op";

    public static final String SINK_SUBSCRIBE_REMOTING_DOMAIN = "subscribe/remoting/domain.op";

    public static final String SINK_HOT_DATA_PUSH_OP = "hot/data/push.op";

    /**
     *
     */
    @Override
    public void onAfterConstructInstance() {

        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(Sink.class)) {
                continue;
            }
            try {
                method.setAccessible(true);
                Sink sink = method.getAnnotation(Sink.class);
                String sinkName = sink.name();
                sinkOperatorMethodMapping.put(sinkName, method);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param sink
     * @param interactive
     */
    @Override
    public void processCustomSink(String sink, IInteractive interactive) {

        Method sinkOperator = sinkOperatorMethodMapping.get(sink);
        if (sinkOperator == null) {
            return;
        }

        try {
            sinkOperator.invoke(this, interactive);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    @Sink(name = SINK_ADD_REMOTING_DOMAIN_OP, desc = "添加一个热点数据 remoting domain")
    public void processAddRemotingDomain(final IInteractive interactive) {

        byte[] remotingDomains = interactive.getInteractivePayload().getPayload().toByteArray();
        final HotDataRemotingDomain hotDataRemotingDomain = WareSwiftDeveloperManager.deserialize(remotingDomains, HotDataRemotingDomain.class);
        hotDataRemotingDomain.setDomainOperator(RemotingDomainManager.DOMAIN_OPERATOR_ADD);
        /**
         * 集群节点间同步
         */
        WareSwiftDeveloperManager.getCapabilityModel().onOutboundDataSet(hotDataRemotingDomain, new OutboundCallback() {
            @Override
            public void onSyncingSuccess(Set<String> channelIdentifies) {

                interactive.sendPayload(WareSwiftDeveloperManager.buildSuccessInteractivePayload(channelIdentifies.toString(), "[ ADD ] hot data [ SUCCESS ]。"));
            }

            @Override
            public void onSyncingFailed(Set<String> successChannelIdentifies, Set<String> failedChannelIdentifies) {

                log.info("success channel identifies=" + successChannelIdentifies + "; failed =" + failedChannelIdentifies.toString());

                InteractivePayload response =
                        WareSwiftDeveloperManager.buildResponseInteractivePayload(("success node size =" + successChannelIdentifies.size() + "; " + " failed is =" + failedChannelIdentifies.size()), "[ ADD ] hot data result", new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.putHeaders(WareSwiftDeveloperManager.RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(WareSwiftDeveloperManager.EXCEPTION_STATUS_CODE));
                            }
                        });
                interactive.sendPayload(response);
            }

            @Override
            public void onMinRequestRequiredAcks(int requestRequiredAcks, int realActiveChannels) {

                InteractivePayload response =
                        WareSwiftDeveloperManager.buildResponseInteractivePayload(("request require ack=" + realActiveChannels + "; " + " real is =" + realActiveChannels), "[ ADD ] hot data failed", new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.putHeaders(WareSwiftDeveloperManager.RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(WareSwiftDeveloperManager.ATTENTION_ACK_STATUS_CODE));
                            }
                        });
                interactive.sendPayload(response);
            }

            @Override
            public void onReplBacklogSyncingAdd(boolean isAddSuccess, RemotingDomainSupport remotingDomainSupport) {
                interactive.sendPayload(
                        InteractivePayload.newBuilder().setPayload(ByteString.copyFrom(("is in replcation ,add replication backlog status=" + isAddSuccess).getBytes(CharsetUtil.UTF_8))).build());
            }

            @Override
            public void onMoved(String nodeIdentify) {
                interactive.sendPayload(
                        InteractivePayload.newBuilder().setPayload(ByteString.copyFrom(nodeIdentify.getBytes(CharsetUtil.UTF_8))).build());
            }
        });
    }

    /**
     * @param interactive
     */
    @Sink(name = "update/remoting/domain.op")
    public void processUpdateRemotingDomain(IInteractive interactive) {
        Map<Integer, String> headsMap = interactive.getInteractivePayload().getHeadersMap();
        String hotDataTopic = headsMap.get(HOT_DATA_TOPIC_HEAD_KEY);
        String hotDataKey = headsMap.get(HOT_DATA_KEY_HEAD_KEY);

        HotDataAvailableCapabilityModel hotDataAvailableCapabilityModel =
                (HotDataAvailableCapabilityModel) WareSwiftDeveloperManager.getCapabilityModel();


        if (hotDataAvailableCapabilityModel.getHotDataRemotingDomain(hotDataTopic, hotDataKey) == null) {

            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setPayload(ByteString.copyFrom((String.format("[ UPDATE ] No corresponding data for [%s] [%s]", hotDataTopic, hotDataKey).getBytes(CharsetUtil.UTF_8))));
            interactive.sendPayload(builder.build());
            return;
        }

        //
        String updateValue = interactive.getInteractivePayload().getPayload().toStringUtf8();
        final HotDataRemotingDomain hotDataRemotingDomain =
                new HotDataRemotingDomain(RemotingDomainManager.DOMAIN_OPERATOR_UPDATE);
        hotDataRemotingDomain.setTopic(hotDataTopic);
        hotDataRemotingDomain.setKey(hotDataKey);
        hotDataRemotingDomain.setValue(updateValue);
        hotDataAvailableCapabilityModel.onOutboundDataSet(hotDataRemotingDomain, new OutboundCallback() {
            @Override
            public void onSyncingSuccess(Set<String> channelIdentifies) {

                interactive.sendPayload(WareSwiftDeveloperManager.buildSuccessInteractivePayload(channelIdentifies.toString(), "[ UPDATE ] hot data [ SUCCESS ]。"));
            }

            @Override
            public void onSyncingFailed(Set<String> successChannelIdentifies, Set<String> failedChannelIdentifies) {

                log.info("success channel identifies=" + successChannelIdentifies + "; failed =" + failedChannelIdentifies.toString());

                InteractivePayload response =
                        WareSwiftDeveloperManager.buildResponseInteractivePayload(("success node size =" + successChannelIdentifies.size() + "; " + " failed is =" + failedChannelIdentifies.size()), "[ UPDATE ] hot data result", new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.putHeaders(WareSwiftDeveloperManager.RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(WareSwiftDeveloperManager.EXCEPTION_STATUS_CODE));
                            }
                        });
                interactive.sendPayload(response);
            }

            @Override
            public void onMinRequestRequiredAcks(int requestRequiredAcks, int realActiveChannels) {

                InteractivePayload response =
                        WareSwiftDeveloperManager.buildResponseInteractivePayload(("request require ack=" + realActiveChannels + "; " + " real is =" + realActiveChannels), "[ UPDATE ] hot data failed", new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.putHeaders(WareSwiftDeveloperManager.RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(WareSwiftDeveloperManager.ATTENTION_ACK_STATUS_CODE));
                            }
                        });
                interactive.sendPayload(response);
            }
        });
    }

    /**
     * @param interactive
     */
    @Sink(name = "delete/remoting/domain.op")
    public void deleteRemotingDomain(IInteractive interactive) {
        HotDataAvailableCapabilityModel hotDataAvailableCapabilityModel =
                (HotDataAvailableCapabilityModel) WareSwiftDeveloperManager.getCapabilityModel();

        java.util.Map<java.lang.Integer, java.lang.String> headersMap = interactive.getInteractivePayload().getHeadersMap();
        String hotDataTopic = headersMap.get(HOT_DATA_TOPIC_HEAD_KEY);
        String hotDataKey = headersMap.get(HOT_DATA_KEY_HEAD_KEY);
        final HotDataRemotingDomain hotDataRemotingDomain = hotDataAvailableCapabilityModel.getHotDataRemotingDomain(hotDataTopic, hotDataKey);
        hotDataRemotingDomain.setDomainOperator(RemotingDomainManager.DOMAIN_OPERATOR_DELETE);
        if (hotDataRemotingDomain == null) {

            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setPayload(ByteString.copyFrom((String.format("[ DELETE ] No corresponding data for [%s] [%s]", hotDataTopic, hotDataKey).getBytes(CharsetUtil.UTF_8))));
            interactive.sendPayload(builder.build());
            return;
        }

        hotDataAvailableCapabilityModel.onOutboundDataSet(hotDataRemotingDomain, new OutboundCallback() {
            @Override
            public void onSyncingSuccess(Set<String> channelIdentifies) {

                interactive.sendPayload(WareSwiftDeveloperManager.buildSuccessInteractivePayload(channelIdentifies.toString(), "[ DELETE ] hot data [ SUCCESS ]。"));
            }

            @Override
            public void onSyncingFailed(Set<String> successChannelIdentifies, Set<String> failedChannelIdentifies) {

                log.info("success channel identifies=" + successChannelIdentifies + "; failed =" + failedChannelIdentifies.toString());
                InteractivePayload response =
                        WareSwiftDeveloperManager.buildResponseInteractivePayload(("success node size =" + successChannelIdentifies.size() + "; " + " failed is =" + failedChannelIdentifies.size()), "[ DELETE ] hot data result", new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.putHeaders(WareSwiftDeveloperManager.RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(WareSwiftDeveloperManager.EXCEPTION_STATUS_CODE));
                            }
                        });
                interactive.sendPayload(response);
            }

            @Override
            public void onMinRequestRequiredAcks(int requestRequiredAcks, int realActiveChannels) {

                InteractivePayload response =
                        WareSwiftDeveloperManager.buildResponseInteractivePayload(("request require ack=" + realActiveChannels + "; " + " real is =" + realActiveChannels), "[ DELETE ] hot data failed", new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.putHeaders(WareSwiftDeveloperManager.RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(WareSwiftDeveloperManager.ATTENTION_ACK_STATUS_CODE));
                            }
                        });
                interactive.sendPayload(response);
            }
        });

    }

    /**
     * @param interactive
     */
    @Sink(name = SINK_SUBSCRIBE_REMOTING_DOMAIN)
    public void subscribeRemotingDomain(IInteractive interactive) {

        String hotDataTopic = interactive.getInteractivePayload().getPayload().toStringUtf8();
        if (StringUtil.isNullOrEmpty(hotDataTopic)) {
            Map<Integer, String> headsMap = interactive.getInteractivePayload().getHeadersMap();
            hotDataTopic = headsMap.get(HOT_DATA_TOPIC_HEAD_KEY);
        }
        subscriberInteractiveRegistry.put(hotDataTopic, interactive);
        interactive.sendPayload(InteractivePayload.newBuilder().setSink(SINK_SUBSCRIBE_REMOTING_DOMAIN).setPayload(ByteString.copyFrom("subscriber success!".getBytes(CharsetUtil.UTF_8))).build());
        log.info("add new hot data topic=" + hotDataTopic + "; subscribe size =" + subscriberInteractiveRegistry.size());
    }

    public void processSubscriberPush(final HotDataRemotingDomain hotDataRemotingDomain) {
        IInteractive subInteractive = subscriberInteractiveRegistry.get(hotDataRemotingDomain.getTopic());
        if (subInteractive == null) {
            return;
        }

        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setPayload(ByteString.copyFrom(WareSwiftDeveloperManager.serialize(hotDataRemotingDomain)));
        builder.setSink(SINK_HOT_DATA_PUSH_OP);
        subInteractive.sendPayload(builder.build());
    }
}