package com.ware.swift.core;

import com.ware.swift.core.inter.ICoreSwiftManager;
import com.ware.swift.core.remoting.RemotingManager;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static com.ware.swift.core.WareCoreSwiftGlobalContext.RAFT_CONTEXT_ATTR_KEY_CONFIG;
import static com.ware.swift.core.WareCoreSwiftGlobalContext.RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH;

/**
 * 
 */
public class AliwareRaftManager implements ICoreSwiftManager {

	private InternalLogger logger = InternalLoggerFactory
			.getInstance(AliwareRaftManager.class);

	public void init(WareCoreSwiftGlobalContext raftGlobalContext) {
		startBefore(raftGlobalContext);
		start(raftGlobalContext);
		startAfter(raftGlobalContext);
	}

	/**
	 * 
	 * @param raftGlobaleContext
	 */
	public void startBefore(WareCoreSwiftGlobalContext raftGlobaleContext) {
		String configPath = raftGlobaleContext.getAttribute(
				RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH, "aliware-raft.properties");
		try {
			WareCoreSwiftConfig raftConfig = WareCoreSwiftConfigManager.initRaftConfig(configPath,
					this.getClass().getClassLoader());
			RemotingManager.getRemotingManager().init(raftConfig);
			raftGlobaleContext.putAttribute(RAFT_CONTEXT_ATTR_KEY_CONFIG, raftConfig);
		}
		catch (Exception e) {
			logger.error(WareCoreSwiftExceptionCode.getConfigInitializerErrorMessage(
					" init raft cause an exception with " + configPath), e);
		}
	}

	/**
	 * 
	 * @param raftGlobaleContext
	 */
	public void start(WareCoreSwiftGlobalContext raftGlobaleContext) {

		RemotingManager.getRemotingManager().startLeaderElection(raftGlobaleContext);
	}

	/**
	 * 
	 * @param raftGlobaleContext
	 */
	public void startAfter(WareCoreSwiftGlobalContext raftGlobaleContext) {

	}

	/**
	 * 
	 * @param nodeInformation
	 */
	public void shutdown(NodeInformation nodeInformation) {

	}

	/**
	 * 
	 */
	public void getAllNodeInformations() {

	}

	/**
	 * 
	 * @param clusterName
	 */
	public void getClusterNodeInformations(String clusterName) {

	}
}
