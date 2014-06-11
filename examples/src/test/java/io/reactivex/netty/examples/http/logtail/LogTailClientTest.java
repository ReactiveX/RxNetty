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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomasz Bak
 */
public class LogTailClientTest {

    private static final int AG_PORT = 8091;
    private static final int PR_FROM_PORT = 8092;
    private static final int PR_TO_PORT = 8095;
    private static final int PR_INTERVAL = 50;
    private static final int TAIL_SIZE = 25;

    private Thread aggregationServer;
    private List<Thread> producerServers = new ArrayList<Thread>();

    @Before
    public void setupServers() {
        for (int i = PR_FROM_PORT; i <= PR_TO_PORT; i++) {
            startProducer(i);
        }
        startAggregator();
    }

    private void startProducer(final int port) {
        Thread producerServer = new Thread(new Runnable() {
            @Override
            public void run() {
                LogProducer.main(new String[]{Integer.toString(port), Integer.toString(PR_INTERVAL)});
            }
        });
        producerServer.start();
        producerServers.add(producerServer);
    }

    private void startAggregator() {
        aggregationServer = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] args = {Integer.toString(AG_PORT), Integer.toString(PR_FROM_PORT), Integer.toString(PR_TO_PORT)};
                LogsAggregator.main(args);
            }
        });
        aggregationServer.start();
    }

    @After
    public void stopServer() {
        if (aggregationServer != null) {
            aggregationServer.interrupt();
        }
        for (Thread t : producerServers) {
            t.interrupt();
        }
    }

    @Test
    public void testLogTailClient() throws Exception {
        LogTailClient client = new LogTailClient(AG_PORT, TAIL_SIZE);
        client.startCollectionProcess();
        List<LogEvent> logs = client.tail();
        Assert.assertEquals(25, logs.size());
    }
}