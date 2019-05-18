package com.ware.swift.event.graph;

import java.util.concurrent.locks.ReentrantLock;

/** 
 * 拓扑排序节点类 
 */  
public class Node<V extends INodeCommand> {
    private V val;  
    private int pathIn = 0; // 入链路数量
    private ReentrantLock lock = new ReentrantLock();
    public Node(V val) {  
        this.val = val;  
    }
	public V getVal() {
		return val;
	}
	public void setVal(V val) {
		lock.lock();
		try{
			this.val = val;
		}finally{
			lock.unlock();
		}
	}
	public int getPathIn() {
		return pathIn;
	}
	public void setPathIn(int pathIn) {
		lock.lock();
		try{
			this.pathIn = pathIn;
		}finally{
			lock.unlock();
		}
	} 
}  
