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

package io.reactivex.netty.examples.http.logtail;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.reactivex.netty.examples.http.logtail.LogTailClient.DEFAULT_TAIL_SIZE;

/**
 * @author Tomasz Bak
 */
public class LogTailClientTest extends ExamplesEnvironment {

    private static final int NUM_OF_PRODUCERS = 4;
    private static final int PR_INTERVAL = 50;

    private HttpServer<ByteBuf, ServerSentEvent> aggregationServer;
    private final List<HttpServer<ByteBuf, ServerSentEvent>> producerServers = new ArrayList<>();
    private final List<Integer> producerPorts = new ArrayList<>();

    @Before
    public void setupServers() {
        for (int i = 0; i <= NUM_OF_PRODUCERS; i++) {
            startProducer();
        }
        startAggregator();
    }

    private void startProducer() {
        HttpServer<ByteBuf, ServerSentEvent> server = new LogProducer(0, PR_INTERVAL).createServer();
        server.start();
        producerServers.add(server);
        producerPorts.add(server.getServerPort());
    }

    private void startAggregator() {
        aggregationServer = new LogAggregator(0, producerPorts).createAggregationServer();
        aggregationServer.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        aggregationServer.shutdown();
        for (HttpServer<ByteBuf, ServerSentEvent> server : producerServers) {
            server.shutdown();
        }
    }

    @Test
    public void testLogTailClient() throws Exception {
        LogTailClient client = new LogTailClient(aggregationServer.getServerPort(), DEFAULT_TAIL_SIZE);
        List<LogEvent> logs = client.collectEventLogs();
        Assert.assertEquals(25, logs.size());
    }
}