package com.ware.swift.event.parallel;

import com.ware.swift.event.common.Log;
import com.ware.swift.event.parallel.action.ActionExecuteException;

/**
 * the different from Action class is that disorder so extends this class will improve throughput 
 * @author pengbingting
 *
 */
public abstract class Command implements Runnable{
	private long createTime ;
	public Command() {
		
		this.createTime = System.currentTimeMillis();
	}
	
	public void run() {
		long start = System.currentTimeMillis();
		try {
			this.execute();
		} catch (Exception e) {
			this.executeException(e.getMessage());
		}
		long end = System.currentTimeMillis();
		long interval = end - start;
		long leftTime = end - createTime;
		
		if (interval >= 1000 || leftTime >= 1100) {
			Log.warn(", interval : " + interval + ", leftTime : " + leftTime + ", size : ");
		}
	}
	
	public void executeException(String execeptionMsg){
		
	}
	
	public abstract void execute() throws ActionExecuteException;
	
	public void executeSuccess(){
		
	}
}
