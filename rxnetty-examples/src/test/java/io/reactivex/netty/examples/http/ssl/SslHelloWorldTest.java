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

package io.reactivex.netty.examples.http.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.http.ssl.SslHelloWorldServer.DEFAULT_PORT;


/**
 * @author Tomasz Bak
 */
public class SslHelloWorldTest extends ExamplesEnvironment {


    private HttpServer<ByteBuf, ByteBuf> server;

    @Before
    public void setupHttpHelloServer() throws Exception {
        server = new SslHelloWorldServer(DEFAULT_PORT).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() throws Exception {
        SslHelloWorldClient client = new SslHelloWorldClient(DEFAULT_PORT);
        HttpResponseStatus responseStatus = client.sendHelloRequest();
        Assert.assertEquals(HttpResponseStatus.OK, responseStatus);
    }
}
