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

package io.reactivex.netty.examples.udp;

import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.udp.server.UdpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.reactivex.netty.examples.udp.HelloUdpServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class HelloUdpTest extends ExamplesEnvironment {

    private UdpServer<DatagramPacket, DatagramPacket> server;

    @Before
    public void setupServer() {
        server = new HelloUdpServer(DEFAULT_PORT).createServer();
        server.start();
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testRequestReplySequence() {
        HelloUdpClient client = new HelloUdpClient(DEFAULT_PORT);
        String reply = client.sendHello();
        Assert.assertEquals(HelloUdpServer.WELCOME_MSG, reply);
    }

}