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

package io.reactivex.netty.examples.http.post;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.http.post.SimplePostClient.MESSAGE;
import static io.reactivex.netty.examples.http.post.SimplePostServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class SimplePostTest extends ExamplesEnvironment {

    private HttpServer<ByteBuf, ByteBuf> server;

    @Before
    public void setupServer() {
        server = new SimplePostServer(DEFAULT_PORT).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testSimplePost() {
        SimplePostClient client = new SimplePostClient(DEFAULT_PORT);
        String replyMessage = client.postMessage();
        Assert.assertEquals(MESSAGE.toUpperCase(), replyMessage);
    }
}