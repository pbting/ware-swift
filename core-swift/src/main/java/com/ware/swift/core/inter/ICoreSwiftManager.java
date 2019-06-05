package com.ware.swift.core.inter;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareSwiftGlobalContext;

/**
 *
 */
public interface ICoreSwiftManager {

    /**
     * @param wareSwiftGlobalContext
     */
    void init(WareSwiftGlobalContext wareSwiftGlobalContext);

    /**
     * @param wareSwiftGlobalContext
     */
    void startBefore(WareSwiftGlobalContext wareSwiftGlobalContext);

    /**
     * @param wareSwiftGlobalContext
     */
    void start(WareSwiftGlobalContext wareSwiftGlobalContext);

    /**
     * @param wareSwiftGlobalContext
     */
    void startAfter(WareSwiftGlobalContext wareSwiftGlobalContext);

    /**
     * @param nodeInformation
     */
    void shutdown(NodeInformation nodeInformation);

    /**
     *
     */
    void getAllNodeInformation();

    /**
     * @param clusterName
     */
    void getClusterNodeInformation(String clusterName);
}
