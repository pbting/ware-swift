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

import com.ware.swift.core.WareSwiftDeveloperManager;

/**
 * @author pbting
 * @date 2019-05-19 10:10 PM
 */
public class HotDataApplication {

    public static void main(String[] args) {

        WareSwiftDeveloperManager.wareSwiftInit("ware-swift.properties");
    }
}