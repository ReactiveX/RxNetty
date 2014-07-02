/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.examples.tcp.interval;

import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.tcp.interval.TcpIntervalServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class TcpIntervalTest extends ExamplesEnvironment {
    private static final int NO_OF_MSG = 3;
    private static final int INTERVAL = 100;

    private RxServer<String, String> server;

    @Before
    public void setupServer() {
        server = new TcpIntervalServer(DEFAULT_PORT, INTERVAL).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() {
        int count = new TcpIntervalClientTakeN(DEFAULT_PORT, NO_OF_MSG).collectMessages();
        Assert.assertEquals(NO_OF_MSG, count);
    }
}