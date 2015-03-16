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

package io.reactivex.netty.examples.tcp.echo;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.text.StringLineDecoder;
import rx.Observable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.examples.tcp.echo.TcpEchoServer.*;

/**
 * @author Nitesh Kant
 */
public final class TcpEchoClient {

    private final int port;

    public TcpEchoClient(int port) {
        this.port = port;
    }

    public List<String> sendEchos() {

        List<String> toReturn = TcpClient.<ByteBuf, ByteBuf>newClient("127.0.0.1", port)
                                         .createConnectionRequest()
                                         .enableWireLogging(LogLevel.ERROR)
                                         .<String, ByteBuf>addChannelHandlerLast("encoder", StringEncoder::new)
                                         .<String, String>addChannelHandlerLast("decoder", StringLineDecoder::new)
                                         .switchMap(connection ->
                                                            connection.write(
                                                                    Observable.interval(1, TimeUnit.SECONDS)
                                                                              .map(aLong -> "Interval: " + aLong + '\n')
                                                                              .take(10))
                                                                      .ignoreElements()
                                                                      .cast(String.class)
                                                                      .mergeWith(connection.getInput()))
                                         .take(10)
                                         .doOnNext(System.out::println)
                                         .toList()
                                         .toBlocking()
                                         .firstOrDefault(null);
        return toReturn;
    }

    public static void main(String[] args) {

        int port = DEFAULT_PORT;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new TcpEchoClient(port).sendEchos();
    }
}
