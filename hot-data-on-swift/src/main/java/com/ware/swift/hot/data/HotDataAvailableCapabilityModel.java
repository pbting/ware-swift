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

import com.ware.swift.core.remoting.*;
import com.ware.swift.core.remoting.avalispart.AbstractAvailableCapabilityModel;
import com.ware.swift.proto.InteractivePayload;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * @author pbting
 * @date 2019-05-19 10:18 AM
 */
public class HotDataAvailableCapabilityModel extends AbstractAvailableCapabilityModel {

    private static final Logger logger = Logger.getLogger(HotDataRemotingDomain.class.getCanonicalName());
    /**
     *
     */
    private final ConcurrentHashMap<String, ConcurrentMap<String, HotDataRemotingDomain>> hotDataRegistry = new ConcurrentHashMap<>();

    /**
     * 1. 当有新的节点加入或者节点重启时，需要对数据进行同步时，可在这里添加您的代码
     *
     * @param syncDataEmitter
     */
    @Override

    public void onDataStreamReplication(DataSyncEmitter syncDataEmitter) {

        hotDataRegistry.values().forEach(stringHotDataRemotingDomainConcurrentMap -> stringHotDataRemotingDomainConcurrentMap.values().forEach(hotDataRemotingDomain -> syncDataEmitter.onEmit(hotDataRemotingDomain)));
        syncDataEmitter.onEmitFinish();
    }

    /**
     * 2. 当一次性同步失败时，系统会用一定的内存空间来持续进行失败节点的重试同步。
     * 当同步超过一定时间后，系统会自动的触发这里的回调，业务层给出具体的处理策略。
     *
     * @param outOfSyncingWaterMarker
     */
    @Override
    public void onOutOfSyncingWaterMarker(Collection<RemotingDomainSupport> outOfSyncingWaterMarker) {

    }

    /**
     * @param remotingDomain
     */
    @Override
    public void onAfterOutboundDataSet(RemotingDomainSupport remotingDomain) {

        HotDataManager.processHotDataRemotingDomain(hotDataRegistry, (HotDataRemotingDomain) remotingDomain);
    }

    /**
     * 3. 集群节点间数据同步时，
     *
     * @param remotingSource
     * @param remotingDomain
     * @param headsMap
     * @throws RemotingInteractiveException
     */
    @Override
    public void onInboundDataSet(final String remotingSource, RemotingDomainSupport remotingDomain, Map<String, String> headsMap) throws RemotingInteractiveException {

        HotDataManager.processHotDataRemotingDomain(hotDataRegistry, (HotDataRemotingDomain) remotingDomain);
    }

    /**
     * 4. 集群节点进行数据同步时，如果构建自定义的 InteractivePayload。
     * <p>
     * 一般来讲，调用{@link com.ware.swift.core.remoting.ClusterDataSyncManager#newSyncInteractivePayload(RemotingDomainSupport, com.ware.swift.core.remoting.IClusterSyncCallbackHoot)} 就够用了。
     * </p>
     *
     * @param remotingDomain
     * @return
     */
    @Override
    public InteractivePayload buildSyncInteractivePayload(RemotingDomainSupport remotingDomain) {

        return ClusterDataSyncManager.newSyncInteractivePayload(remotingDomain, new IClusterSyncCallbackHoot() {
            @Override
            public void callback(InteractivePayload.Builder target) {
                /**
                 *  一般情况下，需要自定义自己的 head,可以在这里进行添加.
                 *  head 提供统一类型的 int 类型而不是字符串，目的是从某种层度来讲统一使用一个 8 字节的类型来表示其具体的 head。
                 *
                 *  而避免当使用字符串是使用参差不齐的长度带来一定内存上的碎片化。
                 */
                target.putHeaders(1001, String.valueOf(System.currentTimeMillis()));
            }
        });
    }

    /**
     * 如果没有具体对应的数据，则返回 null .
     *
     * @param hotDataTopic
     * @param key
     * @return
     */
    public HotDataRemotingDomain getHotDataRemotingDomain(String hotDataTopic, String key) {

        ConcurrentMap<String, HotDataRemotingDomain> keyMap = hotDataRegistry.get(hotDataTopic);
        if (keyMap == null) {
            return null;
        }

        return keyMap.get(key);
    }
}