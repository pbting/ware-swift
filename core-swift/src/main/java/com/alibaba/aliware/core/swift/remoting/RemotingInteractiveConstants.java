package com.alibaba.aliware.core.swift.remoting;

import com.alibaba.aliware.core.swift.inter.ObjectEncodingHandler;
import com.alibaba.aliware.core.swift.inter.ObjectEncodingHandler;
import com.alibaba.aliware.core.swift.inter.ObjectEncodingHandler;

public interface RemotingInteractiveConstants {

	ObjectEncodingHandler OBJECT_ENCODING_HANDLER = new ObjectEncodingHandlerProtobufImpl();

	Integer LEADER_ELECTION_VOTE_ONE = 0x1002;

	/**
	 * Leader 检测 Follower 节点 ，心跳发送失败多少次，即可判断其主观下线的阈值。
	 */
	Integer LEADER_CHECK_FOLLOWER_SDOWN = 3;

	String RAFT_ROLE_PRIFIX = "RAFT";

	/**
	 * peer-to-peer
	 */
	String PTP_ROLE_PRIFIX = "PTP";

	String RECEIVE_LEADER_ACK = "L1";

	String HEARTBEAT_ACK = "L2";

	String HEARTBEAT_RESYNCING = "resyncing";

	String HEARTBEAT_TIMEOUT_COUNT = "L3";

	String HEARTBEAT_CHECK_COUNT = "L4";

	String LEADER_NULL = "Null";

	String PONG = "pong";

	String ROLE_MISMATCH = "L5";
}
