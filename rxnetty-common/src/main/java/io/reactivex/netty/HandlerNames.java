/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.reactivex.netty;

/**
 * A list of all handler names added by the framework. This is just to ensure consistency in naming.
 */
public enum HandlerNames {

    WireLogging("wire-logging-handler"),
    PrimitiveConverter("primitive-converter"),
    ClientReadTimeoutHandler("client-read-timeout-handler"),
    ;

    private final String name;

    HandlerNames(String name) {
        this.name = qualify(name);
    }

    public String getName() {
        return name;
    }

    private static String qualify(String name) {
        return "_rx_netty_" + name;

    }
}
