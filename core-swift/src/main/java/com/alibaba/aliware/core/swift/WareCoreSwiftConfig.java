package com.alibaba.aliware.core.swift;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
public class WareCoreSwiftConfig {

	/**
	 * raft 集群 节点的信息。其中包含当前节点的 ip 和 端口号
	 */
	private List<String> clusterNodes = new LinkedList<>();

	/**
	 * 代表客观下线的 leader 节点
	 */
	private volatile LinkedHashSet<NodeInformation> oDownList = new LinkedHashSet<>();

	private volatile NodeInformation leader;
	/**
	 * 当前节点的配置信息
	 */
	private NodeInformation nodeInformation = new NodeInformation();

	/**
	 * 记录 say hello 发出去的消息包，收回的 ACK 中包含 多少个含有 leader 的信息。
	 *
	 * 根据这个数达到半数以上，才可以进行 node
	 */
	private AtomicInteger sayHelloResponseLeaderCount = new AtomicInteger();

	public List<String> getClusterNodes() {
		return clusterNodes;
	}

	public void setClusterNodes(List<String> clusterNodes) {
		this.clusterNodes = clusterNodes;
	}

	public NodeInformation getNodeInformation() {
		return nodeInformation;
	}

	public Set<NodeInformation> getoDownList() {
		return Collections.unmodifiableSet(oDownList);
	}

	public void addODownNode(NodeInformation odownLeaderNode) {
		oDownList.add(odownLeaderNode);
	}

	public NodeInformation getLeader() {
		return leader;
	}

	public synchronized NodeInformation getAndResetLeader() {
		NodeInformation tmpLeader = leader;
		leader = null;
		return tmpLeader;
	}

	/**
	 * 注意：这里如果是 leader 本身设置的话，还需
	 * @param leader
	 */
	public void setLeader(NodeInformation leader) {
		this.leader = leader;
		this.getNodeInformation().updateAfterAwareLeader(leader);
	}

	public AtomicInteger getSayHelloResponseLeaderCount() {
		return sayHelloResponseLeaderCount;
	}
}
