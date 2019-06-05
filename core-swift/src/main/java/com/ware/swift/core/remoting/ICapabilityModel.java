package com.ware.swift.core.remoting;

import com.ware.swift.core.WareSwiftPluginLoader;
import com.ware.swift.proto.InteractivePayload;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 提供 AP / CP 的能力模型
 */
public interface ICapabilityModel {

    /**
     * 节点接收数据的处理入口。
     *
     * @param remotingSource
     * @param inBoundValue
     * @param {@link         InteractivePayload}
     * @return
     */
    void onInboundDataSet(String remotingSource, RemotingDomainSupport inBoundValue, Map<String, String> headsMap)
            throws RemotingInteractiveException;

    /**
     * 提供数据输出完成后的一个可执行回调的能力。
     *
     * @param outBoundValue
     * @param outboundCallback
     */
    void onOutboundDataSet(RemotingDomainSupport outBoundValue,
                           OutboundCallback outboundCallback);

    /**
     * 数据流同步
     *
     * @param syncDataEmitter
     */
    void onDataStreamReplication(DataSyncEmitter syncDataEmitter);

    /**
     * out of syncing water marker will notify the business.
     *
     * @param outOfSyncingWaterMarker
     */
    default void onOutOfSyncingWaterMarker(
            Collection<RemotingDomainSupport> outOfSyncingWaterMarker) {
        // 不处理的话，就可以不实现该接口。否则可以实现该接口来处理 committed 超时的那些个数据
    }

    default InteractivePayload buildSyncInteractivePayload(RemotingDomainSupport remotingDomain) {

        return ClusterDataSyncManager.newSyncInteractivePayload(remotingDomain);
    }

    /**
     * obtain the {@link ICapabilityModel} from file
     *
     * @param classLoader
     * @return
     */
    static ICapabilityModel getInstance(ClassLoader classLoader) {
        Set<ICapabilityModel> capabilityModels = WareSwiftPluginLoader
                .load(ICapabilityModel.class, classLoader);
        if (capabilityModels == null) {
            // the default model is CP
            throw new IllegalStateException(
                    "the " + ICapabilityModel.class.getName() + " must be set.");
        }

        return capabilityModels.iterator().next();
    }
}
