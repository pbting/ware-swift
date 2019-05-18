package com.alibaba.aliware.grpc.swift.event.swift.parallel.policy;

import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.ActionExecuteException;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.IActionQueue;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.ActionExecuteException;

/**
 * 提交 直接回调的策略
 * @author pengbingting
 *
 */
public class DirectlyCallBackPolicy implements IRejectedActionPolicy {

	@SuppressWarnings("rawtypes")
	@Override
	public void rejectedAction(IActionQueue actionQueue, Action action, ReentrantLock lock) {
		try {
			action.execute();
		} catch (ActionExecuteException e) {
			e.printStackTrace();
		}
	}
}
