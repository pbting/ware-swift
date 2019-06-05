package com.ware.swift.core;

import com.ware.swift.core.inter.ICoreSwiftManager;
import com.ware.swift.core.remoting.RemotingManager;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import static com.ware.swift.core.WareSwiftGlobalContext.RAFT_CONTEXT_ATTR_KEY_CONFIG;
import static com.ware.swift.core.WareSwiftGlobalContext.RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH;

/**
 *
 */
public class CoreSwiftManagerImpl implements ICoreSwiftManager {

    private InternalLogger logger = InternalLoggerFactory
            .getInstance(CoreSwiftManagerImpl.class);

    public void init(WareSwiftGlobalContext wareSwiftGlobalContext) {
        startBefore(wareSwiftGlobalContext);
        start(wareSwiftGlobalContext);
        startAfter(wareSwiftGlobalContext);
    }

    /**
     * @param wareSwiftGlobalContext
     */
    public void startBefore(WareSwiftGlobalContext wareSwiftGlobalContext) {
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        String configPath = wareSwiftGlobalContext.getAttribute(
                RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH, "ware-swift.properties");
        try {
            WareSwiftConfig wareSwiftConfig = WareSwiftConfigManager.initRaftConfig(configPath,
                    this.getClass().getClassLoader());
            RemotingManager.getRemotingManager().init(wareSwiftConfig);
            wareSwiftGlobalContext.putAttribute(RAFT_CONTEXT_ATTR_KEY_CONFIG, wareSwiftConfig);
        } catch (Exception e) {
            logger.error(WareSwiftExceptionCode.getConfigInitializerErrorMessage(
                    " init wareSwift cause an exception with " + configPath), e);
        }
    }

    /**
     * @param wareSwiftGlobalContext
     */
    public void start(WareSwiftGlobalContext wareSwiftGlobalContext) {

        RemotingManager.getRemotingManager().startLeaderElection(wareSwiftGlobalContext);
    }

    /**
     * @param wareSwiftGlobalContext
     */
    public void startAfter(WareSwiftGlobalContext wareSwiftGlobalContext) {

    }

    /**
     * @param nodeInformation
     */
    public void shutdown(NodeInformation nodeInformation) {

    }

    /**
     *
     */
    public void getAllNodeInformation() {

    }

    /**
     * @param clusterName
     */
    public void getClusterNodeInformation(String clusterName) {

    }
}
