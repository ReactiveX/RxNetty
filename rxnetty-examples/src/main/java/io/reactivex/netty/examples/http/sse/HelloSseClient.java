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

package io.reactivex.netty.examples.http.sse;

import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.http.clientNew.HttpClient;
import io.reactivex.netty.protocol.http.clientNew.HttpClientResponse;

/**
 * An <a href="http://www.w3.org/TR/eventsource/">Server sent event</a> "Hello World" example.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server ({@link HelloSseServer}) on
 * an ephemeral port and then send an HTTP request to that server and print the response.
 *
 * <h2>After starting {@link HelloSseServer}</h2>
 *
 * If you want to see how {@link HelloSseServer} work, you can run {@link HelloSseServer} by yourself and then pass
 * the port on which the server started to this class as a program argument:
 *
 <PRE>
    java io.reactivex.netty.examples.http.sse.HelloSseClient [server port]
 </PRE>
 *
 * <h2>Existing HTTP server</h2>
 *
 * You can also use this client to send a GET request "/hello" to an existing HTTP server (different than
 * {@link HelloSseServer}) by passing the port fo the existing server similar to the case above:
 *
 <PRE>
    java io.reactivex.netty.examples.http.sse.HelloSseClient [server port]
 </PRE>
 *
 * In all the above usages, this client will print the response received from the server.
 */
public class HelloSseClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*
         * Retrieves the server port, using the following algorithm:
         * <ul>
             <li>If an argument is passed, then use the argument as the server port.</li>
             <li>Otherwise, see if the server in the passed server class is already running. If so, use that port.</li>
             <li>Otherwise, start the passed server class and use that port.</li>
         </ul>
         */
        int port = getServerPort(HelloSseServer.class, args);

        HttpClient.newClient("localhost", port) /*Create a client*/
                .createGet("/sse") /*Creates a GET request with URI "/hello"*/
                .expectServerSentEvents() /*Enable reading SSE*/
                .doOnNext(resp -> logger.info(resp.toString()))/*Prints the response headers*/
                .flatMap(HttpClientResponse::getContent)
                /*Since, the server sends an infinite stream, take only 10 items*/
                .take(10)
                  /*Block till the response comes to avoid JVM exit.*/
                .toBlocking()
                  /*Print each content chunk*/
                .forEach(sse -> logger.info(sse.toString()));
    }
}
