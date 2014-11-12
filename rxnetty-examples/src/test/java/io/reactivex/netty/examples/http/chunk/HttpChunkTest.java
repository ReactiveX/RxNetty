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

package io.reactivex.netty.examples.http.chunk;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.http.chunk.HttpChunkServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class HttpChunkTest extends ExamplesEnvironment {

    public static final String FILTERED_WORD = "count";
    public static final int WORD_COUNT = 16;
    private static final String TEXT_FILE = HttpChunkTest.class.getClassLoader().getResource("document.txt").getFile();

    private HttpServer<ByteBuf, ByteBuf> server;

    @Before
    public void setupServer() {
        server = new HttpChunkServer(DEFAULT_PORT, TEXT_FILE).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testHttpChunk() throws Exception {
        int count = new HttpChunkClient(DEFAULT_PORT).filterWords(FILTERED_WORD);
        Assert.assertEquals(WORD_COUNT, count);
    }
}
