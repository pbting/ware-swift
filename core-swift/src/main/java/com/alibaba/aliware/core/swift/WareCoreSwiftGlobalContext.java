package com.alibaba.aliware.core.swift;

import com.alibaba.aliware.core.swift.remoting.RemotingManager;

import java.util.Hashtable;

/**
 * 
 */
public class WareCoreSwiftGlobalContext {

	public static final String RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH = "B";

	public static final String RAFT_CONTEXT_ATTR_KEY_CONFIG = "C";

	private Hashtable<String, Object> contextAttrs = new Hashtable<String, Object>();

	public void setConfigPath(String path) {
		contextAttrs.put(RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH, path);
	}

	public <V> void putAttribute(String key, V value) {
		assert key != null;
		contextAttrs.put(key, value);
	}

	public <V> V getAttribute(String key) {
		assert key != null;
		return (V) contextAttrs.get(key);
	}

	public <V> V getAttribute(String key, String defaultValue) {
		assert key != null;
		return (V) contextAttrs.getOrDefault(key, defaultValue);
	}

	public NodeInformation getLeader() {
		if (RemotingManager.getRemotingManager().getRaftConfig() == null) {
			return null;
		}
		return RemotingManager.getRemotingManager().getRaftConfig().getLeader();
	}
}
