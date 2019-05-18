package com.alibaba.aliware.core.swift;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

/**
 * 
 */
public final class WareCoreSwiftConfigManager {

	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(WareCoreSwiftConfigManager.class);

	private static final String CONFIG_SUFIX_XML = "xml";

	private static final String CONFIG_SUFIX_PROPERTIES = "properties";

	public static WareCoreSwiftConfig initRaftConfig(String path, ClassLoader classLoader)
			throws Exception {
		String sufix = path.substring(path.lastIndexOf(".") + 1);
		WareCoreSwiftConfig rafConfig = null;
		switch (sufix) {
		case CONFIG_SUFIX_PROPERTIES: {
			rafConfig = initRaftConfigByProperties(classLoader.getResource(path));
			break;
		}
		}
		return rafConfig;
	}

	private static WareCoreSwiftConfig initRaftConfigByProperties(URL url) throws Exception {
		Properties properties = new Properties();
		properties.load(url.openStream());
		String clusterNodes = properties.getProperty("cluster.nodes");
		if (WareCoreSwiftStringUtils.isBlank(clusterNodes)) {
			throw new WareCoreSwiftException("cluster.nodes must config.");
		}
		String[] clusterNodeArr = clusterNodes.split("[;]");
		WareCoreSwiftConfig raftConfig = new WareCoreSwiftConfig();
		raftConfig.setClusterNodes(Arrays.asList(clusterNodeArr));
		raftConfig.getNodeInformation().setConfig(properties);
		return raftConfig;
	}

}
