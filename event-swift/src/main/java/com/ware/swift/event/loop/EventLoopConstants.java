package com.ware.swift.event.loop;

public interface EventLoopConstants {

	// 1、在事件循序过程中，可通过ObjectEvent.setParameter 来动态调整event loop interval 的时间 间隔。
	String EVENT_LOOP_INTERVAL_PARAM = "event.loop.interval";// 毫秒为单位

}
