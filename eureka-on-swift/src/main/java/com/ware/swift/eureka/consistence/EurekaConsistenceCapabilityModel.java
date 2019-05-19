package com.ware.swift.eureka.consistence;

import com.ware.swift.core.remoting.DataSyncEmitter;
import com.ware.swift.core.remoting.RemotingDomain;
import com.ware.swift.core.remoting.conspart.AbstractConsistenceCapabilityModel;
import com.ware.swift.core.remoting.conspart.ServiceInstance;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务注册中心的基于 raft 的一致性能力模型的实现
 */
public class EurekaConsistenceCapabilityModel extends AbstractConsistenceCapabilityModel {

	/**
	 * 
	 */
	private ConcurrentHashMap<String, ServiceInstance> serviceInstanceRegistry = new ConcurrentHashMap<>(
			128);

	/**
	 * 1. 运行态一个数据 committed 成功之后，会回调这里
	 *
	 * 2. 新加入一个节点，从其他节点同步数据过来也会触发这里的回调。
	 * @param committedRemotingDomains
	 */
	@Override
	public void onCommitted(Collection<RemotingDomain> committedRemotingDomains) {

		if (committedRemotingDomains.isEmpty()) {
			return;
		}
		final ServiceInstance serviceInstance = (ServiceInstance) committedRemotingDomains
				.iterator().next();
		committedRemotingDomains.forEach(remotingDomain -> {
			ServiceInstance committedServiceInstance = (ServiceInstance) remotingDomain;
			ServiceInstance isExist = serviceInstanceRegistry.putIfAbsent(
					committedServiceInstance.getServiceId(), committedServiceInstance);
			if (isExist != null
					&& committedServiceInstance.getVersion() > isExist.getVersion()) {
				// update
				serviceInstanceRegistry.put(committedServiceInstance.getServiceId(),
						committedServiceInstance);
			}
		});

		System.err.println("\t" + serviceInstance.getServiceId() + "; "
				+ serviceInstance.getVersion() + "; size="
				+ serviceInstanceRegistry.size());
	}

	/**
	 * 用来处理那些 out of syncing water marker 的数据。
	 * @param outOfSyncingWaterMarker
	 */
	@Override
	public void onOutOfSyncingWaterMarker(
			Collection<RemotingDomain> outOfSyncingWaterMarker) {

	}

	/**
	 * 这里对已经 committed 的数据进行同步。
	 * @param dataSyncEmitter
	 */
	@Override
	public void onDataStreamSyncing(DataSyncEmitter dataSyncEmitter) {
		// 同步出去
		System.err.println("  -> data size=" + serviceInstanceRegistry.size());
		serviceInstanceRegistry.forEach(
				(key, serviceInstance) -> dataSyncEmitter.onEmit(serviceInstance));

		dataSyncEmitter.onEmitFinish();
	}

}
