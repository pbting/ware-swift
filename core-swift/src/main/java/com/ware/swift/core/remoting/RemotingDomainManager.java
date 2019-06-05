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
package com.ware.swift.core.remoting;

/**
 * @author pbting
 * @date 2019-05-19 10:01 PM
 */
public final class RemotingDomainManager {

    /**
     * 统一提供数据操作的四种{C/R/U/D}可能性。其目的是当有数据变更时向客户端推送时，可以得知当前数据变更的行为来实现不同的业务逻辑。
     * <p>
     * 例如。基于热点数据发现的数据中间件，当某一个热点 topic 的热点数据有新增或者删除或者被修改时，这个时候可以给客户端推送当前操作
     * 的数据项，其中数据项里面就包含四种操作类型中的某一种。
     * </p>
     */


    /**
     * 提供具有数据新增语义的 operator
     */
    public static final byte DOMAIN_OPERATOR_ADD = 1 << 1;

    /**
     * 提供具有数据删除语义的 operator
     */
    public static final byte DOMAIN_OPERATOR_DELETE = 1 << 2;

    /**
     * 提供具有数据变更语义的 operator
     */
    public static final byte DOMAIN_OPERATOR_UPDATE = 1 << 3;

    /**
     * 提供具有数据获取或者查询语义的 operator
     */
    public static final byte DOMAIN_OPERATOR_RETRIEVE = 1 << 4;

    /**
     * @param remotingDomainSupport
     * @return
     */
    public static boolean isAddOperator(RemotingDomainSupport remotingDomainSupport) {

        return (remotingDomainSupport.getDomainOperator() & DOMAIN_OPERATOR_ADD) != 0;
    }

    /**
     * @param remotingDomainSupport
     * @return
     */
    public static boolean isDeleteOperator(RemotingDomainSupport remotingDomainSupport) {

        return (remotingDomainSupport.getDomainOperator() & DOMAIN_OPERATOR_DELETE) != 0;
    }

    /**
     * @param remotingDomainSupport
     * @return
     */
    public static boolean isUpdateOperator(RemotingDomainSupport remotingDomainSupport) {

        return (remotingDomainSupport.getDomainOperator() & DOMAIN_OPERATOR_UPDATE) != 0;
    }

    /**
     * @param remotingDomainSupport
     * @return
     */
    public static boolean isRetrieveOperator(RemotingDomainSupport remotingDomainSupport) {

        return (remotingDomainSupport.getDomainOperator() & DOMAIN_OPERATOR_RETRIEVE) != 0;
    }

}