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

package io.reactivex.netty.examples.http.sse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tomasz Bak
 */
public class HttpSseTest {
    private static final int PORT = 8096;
    private static final int INTERVAL = 100;
    private static final int NO_OF_EVENTS = 10;

    private Thread server;

    @Before
    public void setupHttpSseServer() {
        server = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpSseServer.main(new String[]{Integer.toString(PORT), Integer.toString(INTERVAL)});
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.interrupt();
        }
    }

    @Test
    public void testRequestReplySequence() {
        HttpSseClient client = new HttpSseClient(PORT, NO_OF_EVENTS);
        client.readServerSideEvents();
        Assert.assertEquals(NO_OF_EVENTS, client.events.size());
    }
}
