package com.alibaba.aliware.core.swift;

import java.io.Serializable;

/**
 * 
 */
public interface Information extends Serializable {

	default String identify() {
		return this.getClass().getName();
	}
}
