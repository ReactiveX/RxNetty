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

package io.reactivex.netty.examples.tcp.event;

import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.text.StringLineDecoder;

import static io.reactivex.netty.examples.tcp.event.TcpEventStreamServer.*;

/**
 * @author Nitesh Kant
 */
public final class TcpEventStreamClient {

    private final int port;
    private final int noOfEvents;

    public TcpEventStreamClient(int port, int noOfEvents) {
        this.port = port;
        this.noOfEvents = noOfEvents;
    }

    public int readEvents() {
        return TcpClient.newClient("127.0.0.1", port)
                 .addChannelHandlerLast("decoder", StringLineDecoder::new)
                 .enableWireLogging(LogLevel.ERROR)
                 .createConnectionRequest()
                 .switchMap(Connection::getInput)
                 .take(noOfEvents)
                 .doOnNext(System.out::println)
                 .toList()
                 .toBlocking()
                 .single().size();
    }

    public static void main(String[] args) {
        int delay = 1000;
        int noOfEvents = 100;
        if (args.length > 1) {
            delay = Integer.valueOf(args[0]);
            noOfEvents = Integer.valueOf(args[1]);
        }
        new TcpEventStreamClient(DEFAULT_PORT, noOfEvents).readEvents();
    }
}
