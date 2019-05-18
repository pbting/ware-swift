package com.ware.swift.event.parallel.policy;

import com.ware.swift.event.parallel.action.Action;
import com.ware.swift.event.parallel.action.ActionExecuteException;

import java.util.concurrent.locks.ReentrantLock;

import com.ware.swift.event.parallel.action.IActionQueue;

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
