package com.alibaba.aliware.grpc.swift;

public class GrpcNetExceptionUtils {

	public static boolean isNetUnavailable(Exception e) {
		if (e.getMessage() == null) {
			return false;
		}

		if (e.getMessage().startsWith("UNAVAILABLE")
				&& e.getMessage().indexOf("io exception") != -1) {
			return true;
		}

		if (e.getMessage().indexOf("Connection refused") != -1) {
			return true;
		}

		if (e.getMessage().indexOf("Connection refused") != -1) {
			return true;
		}

		return false;
	}
}
