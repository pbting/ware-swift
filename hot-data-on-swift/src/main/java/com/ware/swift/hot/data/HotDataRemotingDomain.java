/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ware.swift.hot.data;

import com.ware.swift.core.remoting.RemotingDomainSupport;

/**
 * @author pbting
 * @date 2019-05-19 10:57 AM
 */
public class HotDataRemotingDomain extends RemotingDomainSupport {

    private String topic;
    private String key;
    private String value;
    private long reVersion;


    public HotDataRemotingDomain(byte domainOperator) {
        setDomainOperator(domainOperator);
    }

    public HotDataRemotingDomain(String topic, String key, String value) {
        this.topic = topic;
        this.key = key;
        this.value = value;

    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getReVersion() {
        return reVersion;
    }

    public void setReVersion(long reVersion) {
        this.reVersion = reVersion;
    }

    @Override
    public String identify() {
        return key;
    }

    @Override
    public String toString() {
        return "HotDataRemotingDomain{" +
                "topic='" + topic + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", reVersion=" + reVersion +
                '}';
    }
}