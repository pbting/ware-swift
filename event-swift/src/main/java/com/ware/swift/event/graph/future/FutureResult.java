package com.ware.swift.event.graph.future;

public class FutureResult<T> {
	
	private T t ;
	public void setResult(T t){
		synchronized (this) {
			this.t = t ;
			this.notifyAll();
		}
	}
	
	public T getResult(){
		synchronized (this) {
			if(t == null){
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		return this.t;
	}
	
	/**
	 * wait in milliseconds.
	 * @param timeout
	 * @return
	 */
	public T getResult(long timeout){
		synchronized (this) {
			if(t == null){
				try {
					this.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return this.t;
	}
	
}
