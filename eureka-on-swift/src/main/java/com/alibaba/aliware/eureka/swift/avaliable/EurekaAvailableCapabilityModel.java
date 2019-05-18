package com.alibaba.aliware.eureka.swift.avaliable;

import com.alibaba.aliware.core.swift.remoting.DataSyncEmitter;
import com.alibaba.aliware.core.swift.remoting.OutboundCallback;
import com.alibaba.aliware.core.swift.remoting.RemotingDomain;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.core.swift.remoting.avalispart.AbstractAvailableCapabilityModel;
import com.alibaba.aliware.core.swift.remoting.conspart.ServiceInstance;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 基于 AP 模型下的服务注册中心
 */
public class EurekaAvailableCapabilityModel extends AbstractAvailableCapabilityModel {

    /**
     *
     */
    private ConcurrentHashMap<String, ServiceInstance> serviceInstanceRegistry = new ConcurrentHashMap<>(
            128);

    /**
     * 运行态本节点收到其他节点同步过来的数据。
     *
     * @param remotingDomain
     * @param headsMap
     */
    @Override
    public void onInboundDataSet(RemotingDomain remotingDomain,
                                 Map<String, String> headsMap) {
        RemotingManager.getRemotingManager().getRaftConfig().getNodeInformation()
                .incrementCommittedCount();
        ServiceInstance serviceInstance = (ServiceInstance) remotingDomain;
        ServiceInstance oldServiceInstance = serviceInstanceRegistry
                .putIfAbsent(serviceInstance.getServiceId(), serviceInstance);
        if (oldServiceInstance != null
                && oldServiceInstance.getVersion() < serviceInstance.getVersion()) {
            serviceInstanceRegistry.putIfAbsent(serviceInstance.getServiceId(),
                    serviceInstance);
        }

        System.err.println("\t AP 数据大小=" + serviceInstanceRegistry.size());
    }

    /**
     * 运行态，其他节点需要向本节点进行数据同步。
     *
     * @param syncDataEmitter
     */
    @Override
    public void onDataStreamSyncing(DataSyncEmitter syncDataEmitter) {
        serviceInstanceRegistry.forEach(Runtime.getRuntime().availableProcessors(),
                new BiConsumer<String, ServiceInstance>() {
                    @Override
                    public void accept(String s, ServiceInstance serviceInstance) {
                        syncDataEmitter.onEmit(serviceInstance);
                    }
                });
        syncDataEmitter.onEmitFinish();
    }

    @Override
    public void onOutboundDataSet(RemotingDomain remotingDomain,
                                  OutboundCallback callable) {
        RemotingManager.getRemotingManager().getRaftConfig().getNodeInformation()
                .incrementCommittedCount();
        ServiceInstance serviceInstance = (ServiceInstance) remotingDomain;
        ServiceInstance oldServiceInstance = serviceInstanceRegistry
                .putIfAbsent(serviceInstance.getServiceId(), serviceInstance);
        if (oldServiceInstance != null
                && serviceInstance.getVersion() > oldServiceInstance.getVersion()) {
            serviceInstanceRegistry.put(serviceInstance.getServiceId(), serviceInstance);
        }
        System.err.println("\t AP 数据大小=" + serviceInstanceRegistry.size());
        super.onOutboundDataSet(remotingDomain, callable);
    }

    @Override
    public void onOutOfSyncingWaterMarker(
            Collection<RemotingDomain> outOfSyncingWaterMarker) {
        // nothing to do
    }
}
