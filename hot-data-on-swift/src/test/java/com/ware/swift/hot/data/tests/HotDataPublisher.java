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

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareSwiftDeveloperManager;
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.event.ICallbackHook;
import com.ware.swift.hot.data.HotDataRemotingDomain;
import com.ware.swift.proto.InteractivePayload;
import com.ware.swift.rsocket.RSockeetRemotingChannelBuilder;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author pbting
 * @date 2019-05-21 10:38 PM
 */
public class HotDataPublisher {

    @Test
    public void publisherHotData() throws Exception {

        final AbstractRemotingChannel remotingChannel =
                RSockeetRemotingChannelBuilder.newBuilder()
                        .setAddressPort("127.0.0.1:19091")
                        .setClusterName("default").build();

        publishHotData(remotingChannel);
    }

    public NodeInformation getLeaderRSocketRequest(String node) {
        final AbstractRemotingChannel abstractRemotingChannel =
                RSockeetRemotingChannelBuilder.newBuilder()
                        .setAddressPort(node)
                        .setClusterName("default").build();
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSink("getLeader");

        InteractivePayload responsePayload = abstractRemotingChannel
                .requestResponse(builder.build());
        NodeInformation leader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                .decodeResult(responsePayload.getPayload().toByteArray(),
                        NodeInformation.class);
        return leader;
    }

    @Test
    public void getLeaderTests() {
        String node = "127.0.0.1:19092";
        NodeInformation leader = getLeaderRSocketRequest(node);
        System.err.println(leader.toString());
    }

    @Test
    public void consistence() throws Exception {
        String node = "127.0.0.1:19094";
        NodeInformation leader = getLeaderRSocketRequest(node);
        final AbstractRemotingChannel leaderChannel =
                RSockeetRemotingChannelBuilder.newBuilder()
                        .setAddressPort(leader.getAddressPort())
                        .setClusterName("default").build();
        publishHotData(leaderChannel);
        System.in.read();
    }

    public static String randaomeServiceName(String prefix, String suffix) {

        return prefix + "." + UUID.randomUUID().toString().substring(0, 4) + "." + suffix;
    }

    public static void publishHotData(AbstractRemotingChannel leaderChannel) throws InterruptedException {
        final Random random = new Random();
        final int endSize = 100000;
        for (int t = 0; t < 2; t++) {
            new Thread(() -> {
                int size = 1000;
                int totalSize = 0;
                while (totalSize != endSize) {
                    for (int i = 0; i < size; i++) {
                        HotDataRemotingDomain hotDataRemotingDomain = new HotDataRemotingDomain(randaomeServiceName("rsocket", "order"), "consumer.order.services", "com.ware.swift.order.service.BuyGoods");
                        hotDataRemotingDomain.setReVersion(Math.abs(new Random().nextLong()));
                        InteractivePayload interactivePayload = WareSwiftDeveloperManager.buildInteractivePayload("add/remoting/domain.op", hotDataRemotingDomain, new ICallbackHook<InteractivePayload.Builder>() {
                            @Override
                            public void callback(InteractivePayload.Builder target) {
                                target.setSource(leaderChannel.identify());
                            }
                        });
                        try {
                            leaderChannel.requestResponse(interactivePayload);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    totalSize += size;
                    System.err.println("has send data size=" + totalSize);
                    try {
                        TimeUnit.MILLISECONDS.sleep(random.nextInt(500));
                    } catch (InterruptedException e) {
                    }
                }
            }).start();
        }
    }
}