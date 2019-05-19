package com.ware.swift.core.remoting.conspart;

import com.ware.swift.core.remoting.ICapabilityModel;
import com.ware.swift.core.remoting.RemotingDomain;

import java.util.Collection;

/**
 * 具有一致性协议的能力模型
 */
public interface IConsistenceCapabilityModel extends ICapabilityModel {

	/**
	 * 对于 Leader-Follower 架构的系统。 Leader 已经确认所有的 Follower 已经成功接收 committed 的信息。
	 *
	 * <p>
	 * 这个时候应该给业务层一个回调，来处理这个已经到达一致性的数据。
	 * </p>
	 * @param committedRemotingDomains 已经提交成功的一组 remoting domains 集合。
	 */
	void onCommitted(Collection<RemotingDomain> committedRemotingDomains);
}
