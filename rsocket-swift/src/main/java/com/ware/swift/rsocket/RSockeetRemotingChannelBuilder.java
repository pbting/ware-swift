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
package com.ware.swift.rsocket;

import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;

/**
 * @author pbting
 * @date 2019-05-21 10:43 PM
 */
public final class RSockeetRemotingChannelBuilder {

    private String addressPort;
    private String cluterName = "default";

    public static RSockeetRemotingChannelBuilder newBuilder() {
        return new RSockeetRemotingChannelBuilder();
    }

    public RSockeetRemotingChannelBuilder setAddressPort(String addressPort) {

        this.addressPort = addressPort;
        return this;
    }

    public RSockeetRemotingChannelBuilder setClusterName(String clusterName) {
        this.cluterName = clusterName;
        return this;
    }

    public AbstractRemotingChannel build() {
        String[] addressPortArr = addressPort.split("[:]");

        RSocketClientBootstrap clientBootstrap = new RSocketClientBootstrap(
                addressPortArr[0], Integer.valueOf(addressPortArr[1]));

        AbstractRemotingChannel remotingChannel = new RSocketRemotingChannel(
                addressPort, clientBootstrap, AbstractRemotingChannel.JoinType.SYSTEM_STARTUP);
        remotingChannel.setIdentify("CLIENT" + addressPort + "@" + this.cluterName);
        return remotingChannel;
    }
}