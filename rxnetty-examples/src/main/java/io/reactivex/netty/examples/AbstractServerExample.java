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
 */
package io.reactivex.netty.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractServerExample {

    public static final int NOT_STARTED_PORT = -1;

    public static int serverPort = NOT_STARTED_PORT;

    protected static Logger logger = LoggerFactory.getLogger(AbstractServerExample.class);

    public static void mockLogger(Logger logger) {
        AbstractServerExample.logger = logger;
    }

    public static boolean isServerStarted() {
        return NOT_STARTED_PORT != serverPort;
    }

    protected static boolean shouldWaitForShutdown(String[] args) {
        return args.length == 0;
    }

    public static int getServerPort() {
        return serverPort;
    }
}
