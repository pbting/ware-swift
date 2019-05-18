package com.alibaba.aliware.core.swift;

/**
 * each node, must be one of
 */
public enum NodeState {

	Follower, Candidate, Leader, SDOWN, ODOWN, PEER_TO_PEER,
}
