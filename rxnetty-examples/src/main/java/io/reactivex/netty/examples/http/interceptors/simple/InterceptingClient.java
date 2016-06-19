/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.examples.http.interceptors.simple;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.nio.charset.Charset;

import static io.reactivex.netty.examples.http.interceptors.simple.InterceptingServer.*;

/**
 * A client for {@link InterceptingServer}, which follows a simple text based, new line delimited
 * message protocol. It sends an integer as the request payload and expects two integers separated by new lines, as the
 * response payload.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server
 * ({@link InterceptingServer})
 * on an ephemeral port, send a "Hello World!" message to the server and print the response.
 *
 * <h2>After starting {@link InterceptingServer}</h2>
 *
 * If you want to see how {@link InterceptingServer} work, you can run
 * {@link InterceptingServer} by yourself and then
 * pass the port on which the server started to this class as a program argument:
 *
 <PRE>
 java io.reactivex.netty.examples.http.interceptors.simple.InterceptingClient [server port]
 </PRE>
 *
 * <h2>Existing TCP server</h2>
 *
 * You can also use this client to connect to an already running TCP server (different than
 * {@link InterceptingServer}) by passing the port and host of the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.http.interceptors.simple.InterceptingClient [server port] [server host]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 *
 * @see InterceptingServer Default server for this client.
 */
public final class InterceptingClient {

    public static void main(String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(InterceptingClient.class);
        Logger logger = env.getLogger();

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = env.getServerAddress(InterceptingServer.class, args);

        HttpClient.newClient(serverAddress)
                  .enableWireLogging("inter-client", LogLevel.DEBUG)
                  .intercept()
                  .next(provider -> (version, method, uri) -> provider.createRequest(version, method, uri)
                                                                      .addHeader(INTERCEPTOR_HEADER_NAME, "client"))
                  .finish()
                  .createGet("/hello")
                  .doOnNext(resp -> logger.info(resp.toString()))
                  .flatMap((HttpClientResponse<ByteBuf> resp) ->
                                   resp.getContent()
                                       .map(bb -> bb.toString(Charset.defaultCharset()))
                  )
                  .toBlocking()
                  .forEach(logger::info);
    }
}
