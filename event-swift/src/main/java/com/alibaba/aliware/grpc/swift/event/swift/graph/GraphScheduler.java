package com.alibaba.aliware.grpc.swift.event.swift.graph;

import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.AbstractEventObject;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import com.alibaba.aliware.grpc.swift.event.swift.object.AbstractEventObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GraphScheduler extends AbstractEventObject<Node<INodeCommand>> {

	private Graph graph;
	private Map<Node<INodeCommand>, NodeCondition> nodeConditionsMap = null;

	GraphScheduler(Graph graph) {
		this.graph = graph;
		this.initNodeCondition();
	}

	@Override
	public void attachListener() {
		this.addListener(event -> {
			Node<INodeCommand> nodeCmd = event.getValue();
			// 得到当前这个节点的前驱节点[可能有多个]
			Set<Node<INodeCommand>> dependsNodes = (Set<Node<INodeCommand>>) graph
					.getAdjaNode().get(nodeCmd);
			if (dependsNodes != null) {
				// 如果不为null,则向他的后继节点广播一个消息。后继节点都知道自己将收到多少个消息后便可以开始执行
				Iterator<Node<INodeCommand>> iter = dependsNodes.iterator();
				for (; iter.hasNext();) {
					Node<INodeCommand> temp = (Node<INodeCommand>) iter.next();
					NodeCondition nodeCondition = nodeConditionsMap.get(temp);
					nodeCondition.increMessageCount(nodeCmd);// 收到一个消息。进行加一操作。
					nodeCondition.handler();
				}
			}
			else {
				if (nodeConditionsMap.get(nodeCmd) == null) {// 处理只有一个节点的情况
					return;
				}
				nodeConditionsMap.get(nodeCmd).handler();// 如果是最后一个节点每次执行完就直接看消息有没有收足够，收足够了，就执行，没有收足够，就直接丢弃
			}
		}, 4);
	}

	private void initNodeCondition() {
		if (graph == null) {
			return;
		}
		nodeConditionsMap = new ConcurrentHashMap<Node<INodeCommand>, NodeCondition>(
				graph.getVertexSet().size());
		Iterator<Node<INodeCommand>> nodes = graph.getVertexSet().iterator();
		Map<Node<INodeCommand>, Set<Node<INodeCommand>>> reverseAdjaNode = graph
				.getReverseAdjaNode();
		while (nodes.hasNext()) {
			Node<INodeCommand> node = (Node<INodeCommand>) nodes.next();
			Set<Node<INodeCommand>> reverseNode = (Set<Node<INodeCommand>>) reverseAdjaNode
					.get(node);
			nodeConditionsMap.put(node, new NodeCondition(this, node, reverseNode));
		}
	}

	public void notify(Node<INodeCommand> node) {
		ObjectEvent<Node<INodeCommand>> objectEvent = new ObjectEvent<Node<INodeCommand>>(
				this, node, 4);
		this.notifyListeners(objectEvent);
	}
}
