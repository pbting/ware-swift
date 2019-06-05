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
package com.ware.swift.hot.data.tests;

import com.google.protobuf.ByteString;
import com.ware.swift.core.remoting.IRemotingCallStreamObserver;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.hot.data.HotDataClientRequestHandler;
import com.ware.swift.hot.data.HotDataRemotingDomain;
import com.ware.swift.proto.InteractivePayload;
import com.ware.swift.rsocket.RSockeetRemotingChannelBuilder;
import io.netty.util.CharsetUtil;
import org.junit.Test;

/**
 * @author pbting
 * @date 2019-05-21 11:10 PM
 */
public class HotDataSubscriber {

    @Test
    public void subscriber() throws Exception {
        final AbstractRemotingChannel remotingChannel =
                RSockeetRemotingChannelBuilder.newBuilder()
                        .setAddressPort("127.0.0.1:19092")
                        .setClusterName("default").build();

        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSink(HotDataClientRequestHandler.SINK_SUBSCRIBE_REMOTING_DOMAIN);
        builder.setPayload(ByteString.copyFrom("service.registry".getBytes(CharsetUtil.UTF_8)));
        builder.setSource(remotingChannel.identify());
        // 使用服务端推送。
        remotingChannel.requestStream(builder.build()).registryCallback(new IRemotingCallStreamObserver() {
            @Override
            public void onNext(InteractivePayload interactivePayload) {
                String sink = interactivePayload.getSink();
                if (HotDataClientRequestHandler.SINK_SUBSCRIBE_REMOTING_DOMAIN.equals(sink)) {

                    System.out.println("\t subscribe success.");
                } else if (HotDataClientRequestHandler.SINK_HOT_DATA_PUSH_OP.equals(sink)) {
                    HotDataRemotingDomain remotingDomain =
                            (HotDataRemotingDomain) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER.decodeResult(interactivePayload.getPayload().toByteArray(), HotDataRemotingDomain.class);
                    System.err.println("\t\t stream push: " + remotingDomain.toString());
                }
            }
        });
        System.in.read();
    }
}