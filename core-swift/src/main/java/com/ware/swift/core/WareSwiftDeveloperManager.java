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
package com.ware.swift.core;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.ICapabilityModel;
import com.ware.swift.core.remoting.RemotingDomainSupport;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ICallbackHook;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;

import java.util.logging.Logger;

/**
 * 统一面向使用 ware swift 开发者的管理类。
 * <p>
 * 使用该类，你可以获取到当前系统配置的{@link com.ware.swift.core.remoting.ICapabilityModel}
 *
 * @author pbting
 * @date 2019-05-19 9:49 PM
 */
public final class WareSwiftDeveloperManager {

    private static final Logger logger = Logger.getLogger(WareSwiftDeveloperManager.class.getCanonicalName());

    /**
     * 客户端响应的状态码。head 为 100 的为客户端保留的值。建议业务方自己定义自己的key 范围在 1000 及以上。
     */
    public static final Integer RESPONSE_STATUS_CODE_HEAD_KEY = 100;
    /**
     * 客户端响应的原因短语。
     */
    public static final Integer RESPONSE_REASON_PHRASE_HEAD_KEY = 101;

    /**
     * 向客户端返回成功状态码
     */
    public static final Integer SUCCESS_STATUS_CODE = 8200;

    /**
     * 向客户端返回需要注意的提示信息状态。
     * <p>
     * 此状态是表示客户端注意了，当前处理是 ack 小于 {@link NodeInformation#REQUEST_REQUIRED_ACKS}
     */
    public static final Integer ATTENTION_ACK_STATUS_CODE = 8100;


    public static final Integer EXCEPTION_STATUS_CODE = 8500;

    public static boolean isDebug() {

        return RemotingManager.getRemotingManager().getWareSwiftConfig().getNodeInformation().isDebug();
    }

    /**
     * @return
     */
    public static ICapabilityModel getCapabilityModel() {

        return RemotingManager.getRemotingManager().getCapabilityModel();
    }

    public static <V> V getClientPayloadHandler() {

        return (V) RemotingManager.getRemotingManager().getClientInteractivePayloadHandler();
    }

    /**
     * @param successPayload
     * @param reasonPhrase
     * @return
     */
    public static InteractivePayload buildSuccessInteractivePayload(String successPayload, String reasonPhrase) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.putHeaders(RESPONSE_STATUS_CODE_HEAD_KEY, String.valueOf(SUCCESS_STATUS_CODE));
        builder.putHeaders(RESPONSE_REASON_PHRASE_HEAD_KEY, reasonPhrase);
        builder.setPayload(ByteString.copyFrom(successPayload.getBytes(CharsetUtil.UTF_8)));
        return builder.build();
    }

    /**
     * @param payload
     * @param reasonPhrase
     * @return
     */
    public static InteractivePayload buildResponseInteractivePayload(String payload, String reasonPhrase, ICallbackHook<InteractivePayload.Builder> callbackHook) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        callbackHook.callback(builder);
        builder.putHeaders(RESPONSE_REASON_PHRASE_HEAD_KEY, reasonPhrase);
        builder.setPayload(ByteString.copyFrom(payload.getBytes(CharsetUtil.UTF_8)));
        return builder.build();
    }

    /**
     * @param sink
     * @param payload
     * @param callbackHook
     * @param <V>
     * @return
     */
    public static <V> InteractivePayload buildInteractivePayload(String sink, V payload, ICallbackHook<InteractivePayload.Builder> callbackHook) {
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        callbackHook.callback(builder);
        builder.setSink(sink);
        builder.setPayload(ByteString.copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER.encodingResult(payload)));
        return builder.build();
    }

    public static NodeInformation getSelfNodeInformation() {

        return RemotingManager.getRemotingManager().getWareSwiftConfig().getNodeInformation();
    }

    /**
     * @param sink
     * @param payload
     * @param <V>
     * @return
     */
    public static <V> InteractivePayload buildInteractivePayload(String sink, V payload) {

        return buildInteractivePayload(sink, payload, new ICallbackHook.EmptyCallbackHook());
    }

    public static <V> V deserialize(byte[] source, Class<V> clazz) {

        return (V) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER.decodeResult(source, clazz);
    }

    public static byte[] serialize(RemotingDomainSupport remotingDomainSupport) {

        return RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER.encodingResult(remotingDomainSupport).array();
    }

    public static WareSwiftGlobalContext wareSwiftInit(String configFile) {
        long start = System.currentTimeMillis();
        logger.info("start to init ware swift with the config file: " + configFile);
        WareSwiftGlobalContext wareCoreSwiftGlobalContext = WareSwiftGlobalContext.getInstance();
        wareCoreSwiftGlobalContext.setConfigPath(configFile);
        CoreSwiftManagerDelegate.getInstance().init(wareCoreSwiftGlobalContext);
        long end = System.currentTimeMillis();
        logger.info("end to init ware swift and cost time= " + (end - start) + " Ms.");
        return wareCoreSwiftGlobalContext;
    }
}