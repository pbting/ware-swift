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

import com.ware.swift.core.remoting.DataSyncEmitter;
import com.ware.swift.core.remoting.RemotingDomainSupport;
import com.ware.swift.core.remoting.conspart.AbstractConsistenceCapabilityModel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author pbting
 * @date 2019-05-25 12:37 AM
 */
public class HotDataConsistenceCapabilityModel extends AbstractConsistenceCapabilityModel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HotDataRemotingDomain.class.getCanonicalName());
    /**
     *
     */
    private final ConcurrentHashMap<String, ConcurrentMap<String, HotDataRemotingDomain>> hotDataRegistry = new ConcurrentHashMap<>();

    private final AtomicLong committedCount = new AtomicLong();

    /**
     * @param committedRemotingDomains 已经提交成功的一组 remoting domains 集合。
     */
    @Override
    public void onCommitted(Collection<RemotingDomainSupport> committedRemotingDomains) {

        if (committedRemotingDomains == null || committedRemotingDomains.isEmpty()) {
            return;
        }

        committedRemotingDomains.forEach(remotingDomainSupport -> {
            HotDataRemotingDomain hotDataRemotingDomain = (HotDataRemotingDomain) remotingDomainSupport;
            HotDataManager.processHotDataRemotingDomain(hotDataRegistry, hotDataRemotingDomain);
        });
        logger.info("\t data size=" + hotDataRegistry.size());
    }

    /**
     * @param syncDataEmitter
     */
    @Override
    public void onDataStreamReplication(final DataSyncEmitter syncDataEmitter) {
        hotDataRegistry.values().forEach(hotDataCacheRegistry -> {
            hotDataCacheRegistry.values().forEach(hotDataRemotingDomain -> syncDataEmitter.onEmit(hotDataRemotingDomain));
        });

        syncDataEmitter.onEmitFinish();
    }

    /**
     * @param outOfSyncingWaterMarker
     */
    @Override
    public void onOutOfSyncingWaterMarker(Collection<RemotingDomainSupport> outOfSyncingWaterMarker) {

        // nothing to do
    }
}