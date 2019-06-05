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
import com.ware.swift.core.remoting.RemotingInteractiveConstants;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import com.ware.swift.grpc.GrpcRemotingChannelBuilder;
import com.ware.swift.proto.InteractivePayload;
import org.junit.Test;

import java.util.UUID;

/**
 * @author pbting
 * @date 2019-05-21 10:38 PM
 */
public class GrpcHotDataPublisher {

    @Test
    public void publisherHotData() throws Exception {

        final AbstractRemotingChannel remotingChannel =
                GrpcRemotingChannelBuilder.newBuilder()
                        .setAddressPort("127.0.0.1:19091")
                        .setClusterName("default").build();

        HotDataPublisher.publishHotData(remotingChannel);
    }

    public NodeInformation getLeaderRequest(String node) {
        final AbstractRemotingChannel abstractRemotingChannel =
                GrpcRemotingChannelBuilder.newBuilder()
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
        NodeInformation leader = getLeaderRequest(node);
        System.err.println(leader.toString());
    }

    @Test
    public void consistence() throws Exception {
        String node = "127.0.0.1:19092";
        NodeInformation leader = getLeaderRequest(node);
        final AbstractRemotingChannel leaderChannel =
                GrpcRemotingChannelBuilder.newBuilder()
                        .setAddressPort(leader.getAddressPort())
                        .setClusterName("default").build();
        HotDataPublisher.publishHotData(leaderChannel);
        System.in.read();
    }

    public String randaomeServiceName(String prefix, String suffix) {

        return prefix + "." + UUID.randomUUID().toString().substring(0, 4) + "." + suffix;
    }
}