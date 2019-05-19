package com.ware.swift.core.inter;

import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareCoreSwiftGlobalContext;

/**
 * 
 */
public interface ICoreSwiftManager {

	void init(WareCoreSwiftGlobalContext raftGlobalContext);

	/**
	 * 
	 * @param raftGlobaleContext
	 */
	void startBefore(WareCoreSwiftGlobalContext raftGlobaleContext);

	/**
	 * 
	 * @param raftGlobaleContext
	 */
	void start(WareCoreSwiftGlobalContext raftGlobaleContext);

	/**
	 * 
	 * @param raftGlobaleContext
	 */
	void startAfter(WareCoreSwiftGlobalContext raftGlobaleContext);

	/**
	 * 
	 * @param nodeInformation
	 */
	void shutdown(NodeInformation nodeInformation);

	/**
	 * 
	 */
	void getAllNodeInformations();

	/**
	 * 
	 * @param clusterName
	 */
	void getClusterNodeInformations(String clusterName);
}
