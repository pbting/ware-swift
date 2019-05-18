package com.ware.swift.core;

import java.text.MessageFormat;

public final class WareCoreSwiftExceptionCode {

	public final static String CONFIG_NOT_FOUNT_CODE = "RAFT-0001";

	public final static String CONFIG_MALFORMED_URL_CODE = "RAFT-0002";

	public final static String CONFIG_INITIALIZER_ERROR_CODE = "RAFT-0003";

	public final static String REMOTING_STARTUP_SERVER_ERROR_CODE = "RAFT-1001";

	public final static String LEADER_ELECTION_VOTE_ERROR_CODE = "RAFT-1002";

	public final static String REMOTING_BROADCAST_LEADER_ERROR_CODE = "RAFT-1003";

	public final static String REMOTING_OUTBOUND_DATASET_ERROR_CODE = "RAFT-1004";

	public static String getConfigNotFountExceptionMessage(String message) {

		return formatExceptionMessage(CONFIG_NOT_FOUNT_CODE, message);
	}

	public static String getConfigMalformedUrlExceptionMessage(String message) {

		return formatExceptionMessage(CONFIG_MALFORMED_URL_CODE, message);
	}

	public static String getConfigInitializerErrorMessage(String message) {

		return formatExceptionMessage(CONFIG_INITIALIZER_ERROR_CODE, message);
	}

	public static String formatExceptionMessage(String code, String message) {

		return MessageFormat.format("[%s]-%s", code, message);
	}
}
