package com.ware.swift.core;

import java.io.Serializable;

/**
 *
 */
public interface Identify extends Serializable {

    default String identify() {
        return this.getClass().getName();
    }
}
