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

package io.reactivex.netty.examples.http.helloworld;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * An HTTP "Hello World" example. There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server ({@link HelloWorldServer}) on
 * an ephemeral port and then send an HTTP request to that server and print the response.
 *
 * <h2>After starting {@link HelloWorldServer}</h2>
 *
 * If you want to see how {@link HelloWorldServer} work, you can run {@link HelloWorldServer} by yourself and then pass
 * the port on which the server started to this class as a program argument:
 *
 <PRE>
    java io.reactivex.netty.examples.http.helloworld.HelloWorldClient [server port]
 </PRE>
 *
 * <h2>Existing HTTP server</h2>
 *
 * You can also use this client to send a GET request "/hello" to an existing HTTP server (different than
 * {@link HelloWorldServer}) by passing the port and host of the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.http.helloworld.HelloWorldClient [server port] [server host]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 *
 * @see HelloWorldServer Default server for this client.
 */
public class HelloWorldClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = getServerAddress(HelloWorldServer.class, args);

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
