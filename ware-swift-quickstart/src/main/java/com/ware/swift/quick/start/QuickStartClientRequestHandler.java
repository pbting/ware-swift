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
package com.ware.swift.quick.start;

import com.google.protobuf.ByteString;
import com.ware.swift.core.WareSwiftDeveloperManager;
import com.ware.swift.core.remoting.DefaultClientInteractivePayloadHandler;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.OutboundCallback;
import com.ware.swift.proto.InteractivePayload;
import io.netty.util.CharsetUtil;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pbting
 * @date 2019-06-05 10:41 PM
 */
public class QuickStartClientRequestHandler extends DefaultClientInteractivePayloadHandler {

    private final AtomicInteger receiveCount = new AtomicInteger();

    /**
     * 只需重写 process custom sink 方法即可。
     *
     * @param sink
     * @param interactive
     */
    @Override
    public void processCustomSink(String sink, final IInteractive interactive) {
        System.out.println("\t receive count =" + receiveCount.incrementAndGet());
        // 在这里根据客户端请求传入的不同 sink 做相应的处理。
        byte[] remotingDomains = interactive.getInteractivePayload().getPayload().toByteArray();
        final QuickStartRemotingDomain quickStartRemotingDomain = WareSwiftDeveloperManager.deserialize(remotingDomains, QuickStartRemotingDomain.class);

        /**
         * 集群节点间数据同步。
         */
        WareSwiftDeveloperManager.getCapabilityModel().onOutboundDataSet(quickStartRemotingDomain, new OutboundCallback() {
            @Override
            public void onSyncingSuccess(Set<String> channelIdentifies) {

                interactive.sendPayload(InteractivePayload.newBuilder()
                        .setPayload(ByteString.copyFrom(channelIdentifies.toString().getBytes(CharsetUtil.UTF_8))).build());
            }

            @Override
            public void onSyncingFailed(Set<String> successChannelIdentifies, Set<String> failedChannelIdentifies) {

                String responsePayload = "success:" + successChannelIdentifies.toString() + "; fail:" + failedChannelIdentifies.toString();
                interactive.sendPayload(InteractivePayload.newBuilder()
                        .setPayload(ByteString.copyFrom(responsePayload.getBytes(CharsetUtil.UTF_8))).build());
            }
        });
    }
}