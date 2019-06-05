package com.ware.swift.core;

import com.ware.swift.core.inter.ICoreSwiftManager;

import java.util.Set;

/**
 * delegate the core swift manager
 */
public final class CoreSwiftManagerDelegate implements ICoreSwiftManager {

    private static ICoreSwiftManager coreSwiftManager;

    static {
        try {
            Set<ICoreSwiftManager> wareSwiftManagers = WareSwiftPluginLoader.load(ICoreSwiftManager.class,
                    CoreSwiftManagerImpl.class.getClassLoader());

            boolean isLoad = false;
            if (wareSwiftManagers != null && wareSwiftManagers.size() > 0) {
                coreSwiftManager = wareSwiftManagers.iterator().next();
                isLoad = true;
            }

            if (!isLoad) {
                coreSwiftManager = new CoreSwiftManagerImpl();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static CoreSwiftManagerDelegate CORE_SWIFT_MANAGER = new CoreSwiftManagerDelegate();

    private CoreSwiftManagerDelegate() {
    }

    public static CoreSwiftManagerDelegate getInstance() {
        return CORE_SWIFT_MANAGER;
    }

    public void init(WareSwiftGlobalContext wareSwiftGlobalContext) {

        coreSwiftManager.init(wareSwiftGlobalContext);
    }

    public void startBefore(WareSwiftGlobalContext wareSwiftGlobalContext) {
        coreSwiftManager.startBefore(wareSwiftGlobalContext);
    }

    public void start(WareSwiftGlobalContext wareSwiftGlobalContext) {
        coreSwiftManager.start(wareSwiftGlobalContext);
    }

    public void startAfter(WareSwiftGlobalContext wareSwiftGlobalContext) {
        coreSwiftManager.startAfter(wareSwiftGlobalContext);
    }

    public void shutdown(NodeInformation nodeInformation) {
        coreSwiftManager.shutdown(nodeInformation);
    }

    public void getAllNodeInformation() {
        coreSwiftManager.getAllNodeInformation();
    }

    public void getClusterNodeInformation(String clusterName) {
        coreSwiftManager.getClusterNodeInformation(clusterName);
    }
}
