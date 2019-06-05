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
package com.ware.swift.core.remoting;

import com.ware.swift.core.WareSwiftPluginLoader;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannel;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author pbting
 * @date 2019-05-24 12:19 AM
 */
public interface ITransactionModel {

    /**
     * @param remotingDomainWrapper
     * @return
     */
    int addSyncingRemotingDomain(
            RemotingDomainWrapper remotingDomainWrapper);

    /**
     * @param remotingDomainWrapper
     * @return
     */
    int addSemiSyncingRemotingDomain(RemotingDomainWrapper remotingDomainWrapper);

    /**
     * @return
     */
    Collection<RemotingDomainWrapper> getAndClearSemiSyncingRemotingDomainIds();

    /**
     * @param visitor
     */
    void visitSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor);

    /**
     * 当前节点是否处于 数据流同步的状态
     *
     * @return
     */
    boolean isInDataStreamReplication();

    /**
     *
     */
    void processReplBacklogSyncingQueue();

    /**
     * @return
     */
    int getDataStreamReplicationCount();

    /**
     *
     */
    int dataStreamReplicationIncre();

    /**
     *
     */
    int dataStreamReplicationDecre();

    /**
     * @return
     */
    boolean getDecentralizeBacklogSwitch();

    /**
     * @return
     */
    boolean startDecentralizeBacklogSwitch();


    /**
     * @return
     */
    boolean closeDecentralizeBacklogSwitch();

    /**
     *
     */
    void setDataStreamReplicationCount(int count);

    /**
     * @param visitor
     */
    void visitSemiSyncingRemotingDomain(Consumer<RemotingDomainWrapper> visitor);

    /**
     * @param remotingDomainWrapper
     */
    void removeSyncingRemotingDomain(RemotingDomainWrapper remotingDomainWrapper);

    /**
     * @param remotingDomainWrapper
     * @return
     */
    boolean addReplBacklogSyncingQueue(RemotingDomainSupport remotingDomainWrapper);

    /**
     * @param committedIds
     * @param remotingChannel
     */
    Collection<RemotingDomainSupport> getCommittedRemotingDomains(
            Collection<String> committedIds, AbstractRemotingChannel remotingChannel);

    /**
     * @param channelSource
     * @param term
     * @return
     */
    String prepareCommittedRemotingDomains(String channelSource, long term, String... semiSyncingRemotingDomainIds);

    /**
     * @return
     */
    int getSyncingRemotingDomainSize();

    /**
     *
     */
    void clearSyncingRemotingDomainIds();

    /**
     * @return
     */
    Collection<RemotingDomainWrapper> getAndClearSyncingRemotingDomainIds();

    /**
     * @param term
     * @return
     */
    Collection<RemotingDomainSupport> committedSyncingDomains(long term);

    /**
     * obtain the {@link ICapabilityModel} from file
     *
     * @param classLoader
     * @return
     */
    static ITransactionModel getInstance(ClassLoader classLoader) {
        Set<ITransactionModel> capabilityModels = WareSwiftPluginLoader
                .load(ITransactionModel.class, classLoader);
        if (capabilityModels == null || capabilityModels.isEmpty()) {
            // the default model is CP
            return new MemoryTransactionModel();
        }

        return capabilityModels.iterator().next();
    }
}
