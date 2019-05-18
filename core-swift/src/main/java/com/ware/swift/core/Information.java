package com.ware.swift.core;

import java.io.Serializable;

/**
 * 
 */
public interface Information extends Serializable {

	default String identify() {
		return this.getClass().getName();
	}
}
