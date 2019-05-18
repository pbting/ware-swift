package com.ware.swift.core;

import com.ware.swift.core.inter.ICoreSwiftManager;

import java.util.Set;

/**
 * 
 */
public final class CoreSwiftManager implements ICoreSwiftManager {

	private static ICoreSwiftManager iRaftManager;

	static {
		try {
			Set<ICoreSwiftManager> raftManagers = WareCoreSwiftPluginLoader.load(ICoreSwiftManager.class,
					AliwareRaftManager.class.getClassLoader());

			boolean isLoad = false;
			if (raftManagers != null && raftManagers.size() > 0) {
				iRaftManager = raftManagers.iterator().next();
				isLoad = true;
			}

			if (!isLoad) {
				iRaftManager = new AliwareRaftManager();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final static CoreSwiftManager ALIWARE_RAFT_CORE = new CoreSwiftManager();

	private CoreSwiftManager() {
	}

	public static CoreSwiftManager getInstance() {
		return ALIWARE_RAFT_CORE;
	}

	public void init(WareCoreSwiftGlobalContext raftGlobalContext) {

		iRaftManager.init(raftGlobalContext);
	}

	public void startBefore(WareCoreSwiftGlobalContext raftGlobalContext) {
		iRaftManager.startBefore(raftGlobalContext);
	}

	public void start(WareCoreSwiftGlobalContext raftGlobalContext) {
		iRaftManager.start(raftGlobalContext);
	}

	public void startAfter(WareCoreSwiftGlobalContext raftGlobalContext) {
		iRaftManager.startAfter(raftGlobalContext);
	}

	public void shutdown(NodeInformation nodeInformation) {
		iRaftManager.shutdown(nodeInformation);
	}

	public void getAllNodeInformations() {
		iRaftManager.getAllNodeInformations();
	}

	public void getClusterNodeInformations(String clusterName) {
		iRaftManager.getClusterNodeInformations(clusterName);
	}
}
