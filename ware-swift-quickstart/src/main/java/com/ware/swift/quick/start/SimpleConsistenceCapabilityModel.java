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

import com.ware.swift.core.WareSwiftDeveloperManager;
import com.ware.swift.core.remoting.DataSyncEmitter;
import com.ware.swift.core.remoting.RemotingDomainSupport;
import com.ware.swift.core.remoting.conspart.AbstractConsistenceCapabilityModel;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author pbting
 * @date 2019-05-25 1:26 AM
 */
public class SimpleConsistenceCapabilityModel extends AbstractConsistenceCapabilityModel {

    private final ConcurrentMap<String, RemotingDomainSupport> receiveDataSets = new ConcurrentHashMap<>();

    /**
     * @param committedRemotingDomains 已经提交成功的一组 remoting domains 集合。
     */
    @Override
    public void onCommitted(Collection<RemotingDomainSupport> committedRemotingDomains) {
        if (committedRemotingDomains == null || committedRemotingDomains.isEmpty()) {
            return;
        }

        committedRemotingDomains.forEach(remotingDomainSupport -> receiveDataSets.put(remotingDomainSupport.identify(), remotingDomainSupport));
        System.out.println(WareSwiftDeveloperManager.getSelfNodeInformation().identify() + "\t receive data size=" + receiveDataSets.size());
    }

    /**
     * @param syncDataEmitter
     */
    @Override
    public void onDataStreamReplication(DataSyncEmitter syncDataEmitter) {
        for (RemotingDomainSupport remotingDomainSupport : receiveDataSets.values()) {
            syncDataEmitter.onEmit(remotingDomainSupport);
        }
    }
}