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
 *
 */

package io.reactivex.netty.examples.http.perf;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.examples.http.helloworld.HelloWorldClient;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * A client to test {@link PerfHelloWorldServer}. This client is provided here only for completeness of the example,
 * otherwise, it is exactly the same as {@link HelloWorldClient}.
 */
public class PerfHelloWorldClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = getServerAddress(PerfHelloWorldServer.class, args);

        /*Create a new client for the server address*/
        HttpClient.newClient(serverAddress)
                .enableWireLogging(LogLevel.DEBUG)
                  /*Creates a GET request with URI "/hello"*/
                .createGet("/hello")
                  /*Prints the response headers*/
                .doOnNext(resp -> logger.info(resp.toString()))
                  /*Since, we are only interested in the content, now, convert the stream to the content stream*/
                .flatMap((HttpClientResponse<ByteBuf> resp) ->
                                 resp.getContent()
                                     /*Convert ByteBuf to string for each content chunk*/
                                         .map(bb -> bb.toString(Charset.defaultCharset()))
                )
                  /*Block till the response comes to avoid JVM exit.*/
                .toBlocking()
                  /*Print each content chunk*/
                .forEach(logger::info);
    }
}
