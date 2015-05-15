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

package io.reactivex.netty.examples.tcp.secure;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;

import java.nio.charset.Charset;

/**
 * A TCP client for {@link SecureEchoServer}
 */
public final class SecureEchoClient extends AbstractClientExample {

    public static void main(String[] args) {

        int port = getServerPort(SecureEchoServer.class, args);

        TcpClient.<ByteBuf, ByteBuf>newClient("127.0.0.1", port)
                 .unsafeSecure()
                 //.enableWireLogging(LogLevel.ERROR)
                 .createConnectionRequest()
                 .flatMap(connection ->
                                  connection.writeString(Observable.just("Hello World!"))
                                            .ignoreElements()
                                            .cast(ByteBuf.class)
                                            .mergeWith(connection.getInput().take(1))
                                            .concatWith(connection.close()
                                                                  .cast(ByteBuf.class))
                 )
                 .map(bb -> bb.toString(Charset.defaultCharset()))
                 .toBlocking()
                 .forEach(logger::error);
    }
}
