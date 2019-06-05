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
package com.ware.swift.grpc;

import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

/**
 * @author pbting
 * @date 2019-05-21 10:43 PM
 */
public final class GrpcRemotingChannelBuilder {

    private String addressPort;
    private String clusterName = "default";

    public static GrpcRemotingChannelBuilder newBuilder() {
        return new GrpcRemotingChannelBuilder();
    }

    public GrpcRemotingChannelBuilder setAddressPort(String addressPort) {

        this.addressPort = addressPort;
        return this;
    }

    public GrpcRemotingChannelBuilder setClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public AbstractRemotingChannel build() {
        String[] addressPortArr = addressPort.split("[:]");
        NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder.forAddress(
                addressPortArr[0].trim(), Integer.valueOf(addressPortArr[1].trim()));

        ManagedChannel channel = nettyChannelBuilder.usePlaintext()
                .flowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
                .build();
        GrpcRemotingChannel remotingChannel = new GrpcRemotingChannel(channel,
                addressPort, AbstractRemotingChannel.JoinType.SYSTEM_STARTUP);
        remotingChannel.setIdentify("CLIENT@" + addressPort + "@" + clusterName);

        return remotingChannel;
    }
}