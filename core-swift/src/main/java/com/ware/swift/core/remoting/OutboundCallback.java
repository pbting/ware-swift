package com.ware.swift.core.remoting;

import com.ware.swift.core.NodeInformation;

import java.util.Set;

/**
 * 数据在集群节点间同步后的回调。提供以下几种基本的语义:
 *
 * 1. 成功同步后的回调。同时会给出成功同步后的节点数，以及同步的具体节点信息
 *
 * 2. 同步失败。
 *
 * 2.1 存活的连接数小于
 * {@link NodeInformation#getRequestRequiredAcks(int)},此时也会给业务方一个回调处理的结果
 *
 * 2.2
 * 开始同步时发现存活的连接已经达到{@link NodeInformation#REQUEST_REQUIRED_ACKS},
 *
 * 但是同步过程中，发现部分节点失败，导致最后部分节点同步成功。
 */
public interface OutboundCallback {

	/**
	 * @param channelIdentifys 每个 remoting channel 的 identify
	 */
	default void onSyncingSuccess(Set<String> channelIdentifys) {
		// nothing to do
	}

	/**
	 * 注意: 发生此方法的回调已经进行了数据的同步。
	 * @param successChannelIdentifys 同步成功的 remoting channel
	 * @param failedChannelIdentifys 同步失败的 remoting channel
	 */
	default void onSyncingFailed(Set<String> successChannelIdentifys,
			Set<String> failedChannelIdentifys) {
		/**
		 * nothing to do
		 */
	}

	/**
	 * 注意: 当发生此方法的回调时，还没进行数据的同步
	 * @param requestRequiredAcks 请求需要的 ACK 数
	 * @param realActiveChannels 实际存活的连接数
	 */
	default void onMinRequestRequiredAcks(int requestRequiredAcks,
			int realActiveChannels) {
		/**
		 * nothing to do
		 */
	}

	/**
	 * 当收到的请求发送当前节点感知到 leader 的 term 和实际的 leader 的 term 低时，应该通知客户端更新最新的 leader 信息。
	 * @param nodeIdentify
	 */
	default void onMoved(String nodeIdentify) {
		/**
		 * nothing to do
		 */
	}
}
