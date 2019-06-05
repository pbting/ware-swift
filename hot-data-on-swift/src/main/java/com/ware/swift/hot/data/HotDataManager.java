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

import com.ware.swift.core.WareSwiftDeveloperManager;
import com.ware.swift.core.remoting.RemotingDomainManager;
import com.ware.swift.event.mcache.AbstractConcurrentCache;
import com.ware.swift.event.mcache.IExpireKeyHandler;
import com.ware.swift.event.mcache.LRFUByDxConcurrentCache;

import java.util.concurrent.ConcurrentMap;

/**
 * @author pbting
 * @date 2019-05-25 12:43 AM
 */
public final class HotDataManager {

    /**
     * @param remotingDomain
     */
    public static void processHotDataRemotingDomain(ConcurrentMap<String, ConcurrentMap<String, HotDataRemotingDomain>> hotDataRegistry, HotDataRemotingDomain remotingDomain) {
        final HotDataRemotingDomain hotDataRemotingDomain = remotingDomain;

        if (WareSwiftDeveloperManager.isDebug()) {
            HotDataClientRequestHandler hotDataClientRequestHandler = WareSwiftDeveloperManager.getClientPayloadHandler();
            hotDataClientRequestHandler.processSubscriberPush(remotingDomain);
        }

        AbstractConcurrentCache abstractConcurrentCache = new LRFUByDxConcurrentCache<>(hotDataRemotingDomain.getTopic(), new HotDataExpireKeyHandler(), 1024);
        ConcurrentMap<String, HotDataRemotingDomain> concurrentMap = hotDataRegistry.putIfAbsent(hotDataRemotingDomain.getTopic(), abstractConcurrentCache);

        if (concurrentMap == null) {
            concurrentMap = abstractConcurrentCache;
        }

        if (RemotingDomainManager.isAddOperator(remotingDomain) || RemotingDomainManager.isUpdateOperator(remotingDomain)) {
            HotDataRemotingDomain preHotDataRemotingDomain = concurrentMap.putIfAbsent(hotDataRemotingDomain.getKey(), hotDataRemotingDomain);
            boolean isPush = true;
            if (preHotDataRemotingDomain != null) {
                if (preHotDataRemotingDomain.getReVersion() < hotDataRemotingDomain.getReVersion()) {
                    concurrentMap.put(hotDataRemotingDomain.getKey(), hotDataRemotingDomain);
                    isPush = true;
                } else {
                    isPush = false;
                }
            }

            if (isPush) {
                HotDataClientRequestHandler hotDataClientRequestHandler = WareSwiftDeveloperManager.getClientPayloadHandler();
                hotDataClientRequestHandler.processSubscriberPush(hotDataRemotingDomain);
            }
            return;
        }

        if (RemotingDomainManager.isDeleteOperator(remotingDomain)) {

            concurrentMap.remove(remotingDomain.getKey());
            // 数据发生变更，推送到订阅的客户端。
            HotDataClientRequestHandler hotDataClientRequestHandler = WareSwiftDeveloperManager.getClientPayloadHandler();
            hotDataClientRequestHandler.processSubscriberPush(remotingDomain);
            return;
        }
    }

    /**
     *
     */
    public static class HotDataExpireKeyHandler implements IExpireKeyHandler<String, HotDataRemotingDomain> {

        @Override
        public void expire(String key, HotDataRemotingDomain value, AbstractConcurrentCache cache) {
            value.setDomainOperator(RemotingDomainManager.DOMAIN_OPERATOR_DELETE);
            HotDataClientRequestHandler hotDataClientRequestHandler = WareSwiftDeveloperManager.getClientPayloadHandler();
            hotDataClientRequestHandler.processSubscriberPush(value);
        }
    }
}