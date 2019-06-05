package com.ware.swift.core.remoting;

import com.ware.swift.core.inter.ObjectEncodingHandler;

public interface RemotingInteractiveConstants {

    long DEFAULT_SYNCING_TERM = -1;
    int HEADER_KEY_TERM_VALUE = 1;
    int HEADER_KEY_REMOTING_DOMAIN_CLASS = 2;
    int HEADER_KEY_SYNCING_REMOTING_DOMAIN_ID = 3;
    int HEADER_KEY_SEMI_SYNCING_REMOTING_DOMAIN_ID = 4;
    int HEADER_KEY_DATA_STREAM_REPLICATION = 5;
    int HEADER_KEY_CLUSTER_NODES = 6;
    int HEADER_KEY_DATA_STREAM_REPLICATION_COUNT = 7;

    ObjectEncodingHandler OBJECT_ENCODING_HANDLER = new ObjectEncodingHandlerProtobufImpl();

    Integer LEADER_ELECTION_VOTE_ONE = 0x1002;

    String SYNCING_IDS_SEPARATOR = ";";

    String RAFT_ROLE_PREFIX = "RAFT";

    /**
     * peer-to-peer
     */
    String PTP_ROLE_PREFIX = "PTP";

    String RECEIVE_LEADER_ACK = "L1";

    String HEARTBEAT_TIMEOUT_COUNT = "L3";

    String HEARTBEAT_CHECK_COUNT = "L4";

    String PONG = "pong";

    String ROLE_MISMATCH = "L5";
}
