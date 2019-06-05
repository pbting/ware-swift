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

import com.ware.swift.core.remoting.DataSyncEmitter;
import com.ware.swift.core.remoting.RemotingDomainSupport;
import com.ware.swift.core.remoting.RemotingInteractiveException;
import com.ware.swift.core.remoting.avalispart.AbstractAvailableCapabilityModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author pbting
 * @date 2019-06-05 10:29 PM
 */
public class SimpleAvailableCapabilityModel extends AbstractAvailableCapabilityModel {

    private final ConcurrentMap<String, RemotingDomainSupport> receiveDataSets = new ConcurrentHashMap<>();

    /**
     * 收到其他节点同步过来的数据
     *
     * @param remotingSource
     * @param inBoundValue
     * @param headsMap
     * @throws RemotingInteractiveException
     */
    @Override
    public void onInboundDataSet(String remotingSource, RemotingDomainSupport inBoundValue, Map<String, String> headsMap) throws RemotingInteractiveException {

        receiveDataSets.put(inBoundValue.identify(), inBoundValue);
    }

    /**
     * 其他节点刚启动时，需要向本节点同步数据。
     *
     * @param syncDataEmitter
     */
    @Override
    public void onDataStreamReplication(DataSyncEmitter syncDataEmitter) {

        receiveDataSets.values().forEach(remotingDomainSupport -> syncDataEmitter.onEmit(remotingDomainSupport));
    }


    /**
     * 本节点数据同步到其他节点后的回调
     *
     * @param remotingDomain
     */
    @Override
    public void onAfterOutboundDataSet(RemotingDomainSupport remotingDomain) {

        receiveDataSets.put(remotingDomain.identify(), remotingDomain);
    }
}