package com.ware.swift.core;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 */
public final class WareSwiftConfigManager {

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(WareSwiftConfigManager.class);

    private static final String CONFIG_SUFFIX_PROPERTIES = "properties";

    public static WareSwiftConfig initRaftConfig(String path, ClassLoader classLoader)
            throws Exception {
        String suffix = path.substring(path.lastIndexOf(".") + 1);
        WareSwiftConfig rafConfig = null;
        switch (suffix) {
            case CONFIG_SUFFIX_PROPERTIES: {
                rafConfig = initRaftConfigByProperties(classLoader.getResource(path));
                break;
            }
        }
        return rafConfig;
    }

    private static WareSwiftConfig initRaftConfigByProperties(URL url) throws Exception {
        Properties properties = new Properties();
        properties.load(url.openStream());
        String clusterNodes = properties.getProperty("cluster.nodes");
        if (WareSwiftStringUtils.isBlank(clusterNodes)) {
            throw new WareSwiftException("cluster.nodes must config.");
        }
        String[] clusterNodeArr = clusterNodes.split("[;]");
        WareSwiftConfig wareSwiftConfig = new WareSwiftConfig();
        wareSwiftConfig.setClusterNodes(Arrays.asList(clusterNodeArr));
        wareSwiftConfig.getNodeInformation().setConfig(properties);
        return wareSwiftConfig;
    }

}
