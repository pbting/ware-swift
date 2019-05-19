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
package com.ware.swift.eureka.tests;

import org.junit.Test;

import com.ware.swift.core.CoreSwiftManager;
import com.ware.swift.core.NodeInformation;
import com.ware.swift.core.WareCoreSwiftGlobalContext;
import java.util.concurrent.TimeUnit;

/**
 * @author pbting
 * @date 2019-05-18 3:12 AM
 */
public class WareSwiftNodeStartTests {

    @Test
    public void wareSwiftStartup() throws Exception {
        WareCoreSwiftGlobalContext raftGlobalContext = new WareCoreSwiftGlobalContext();
        raftGlobalContext.setConfigPath("ware-swift.properties");
        CoreSwiftManager.getInstance().init(raftGlobalContext);

        while (true) {
            TimeUnit.SECONDS.sleep(1);

            NodeInformation leader = raftGlobalContext.getLeader();
            if (leader == null) {
                System.out.println("does not election...");
                continue;
            }
        }
    }
}