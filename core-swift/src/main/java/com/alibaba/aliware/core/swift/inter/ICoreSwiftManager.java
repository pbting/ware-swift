package com.alibaba.aliware.core.swift.inter;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.WareCoreSwiftGlobalContext;

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
