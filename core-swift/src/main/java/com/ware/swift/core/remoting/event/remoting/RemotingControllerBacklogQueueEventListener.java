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
package com.ware.swift.core.remoting.event.remoting;

import com.ware.swift.core.remoting.ClusterDataSyncManager;
import com.ware.swift.core.remoting.IInteractive;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.proto.InteractivePayload;

/**
 * @author pbting
 * @date 2019-05-28 1:57 AM
 */
public class RemotingControllerBacklogQueueEventListener extends AbstractRemotingPipelineEventListener {

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive interactive = event.getValue();

        String payload = interactive.getInteractivePayload().getPayload().toStringUtf8();

        if (ClusterDataSyncManager.DATA_STREAM_REPLICATION_REASON_PHRASE_START.equals(payload)) {
            // 启动
            RemotingManager.getRemotingManager().getTransactionModel().startDecentralizeBacklogSwitch();
        } else if (ClusterDataSyncManager.DATA_STREAM_REPLICATION_REASON_PHRASE_COMPLETE.equals(payload)) {
            // 关闭
            boolean backlogSwitch = RemotingManager.getRemotingManager().getTransactionModel().closeDecentralizeBacklogSwitch();
            if (backlogSwitch) {
                RemotingManager.getRemotingManager()
                        .getTransactionModel().processReplBacklogSyncingQueue();
            }
        }

        interactive.sendPayload(InteractivePayload.newBuilder()
                .setSource(interactive.getInteractivePayload().getSource())
                .setPayload(interactive.getInteractivePayload().getPayload()).build());
        return true;
    }
}