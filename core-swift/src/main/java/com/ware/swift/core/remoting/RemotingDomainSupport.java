package com.ware.swift.core.remoting;

import com.ware.swift.core.Identify;

import java.io.Serializable;

/**
 * 用于两个进程间进行通信的 object
 */
public abstract class RemotingDomainSupport implements Serializable, Identify {

    /**
     * the default operator
     */
    public byte domainOperator = RemotingDomainManager.DOMAIN_OPERATOR_ADD;

    public RemotingDomainSupport() {

    }

    public RemotingDomainSupport(byte domainOperator) {
        this.domainOperator = domainOperator;
    }

    public byte getDomainOperator() {
        return domainOperator;
    }

    public void setDomainOperator(byte domainOperator) {
        this.domainOperator = domainOperator;
    }
}
