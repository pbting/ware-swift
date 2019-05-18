package com.ware.swift.core.remoting.conspart;

import com.ware.swift.core.remoting.RemotingDomain;

/**
 * 
 */
public class ServiceInstance implements RemotingDomain {

	private String serviceId;
	private String host;
	private int port;
	private boolean secure;
	private String scheme;
	private long version;

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "ServiceInstance{" + "serviceId='" + serviceId + '\'' + ", host='" + host
				+ '\'' + ", port=" + port + ", secure=" + secure + ", scheme='" + scheme
				+ '\'' + ", version=" + version + '}';
	}
}
