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
package io.reactivex.netty.examples.http.pipelining;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * An HTTP pipelining example. There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server ({@link PipeliningServer}) on
 * an ephemeral port and then send an HTTP request to that server and print the response.
 *
 * <h2>After starting {@link PipeliningServer}</h2>
 *
 * If you want to see how {@link PipeliningServer} work, you can run {@link PipeliningServer} by yourself and then pass
 * the port on which the server started to this class as a program argument:
 *
 <PRE>
 java io.reactivex.netty.examples.http.pipelining.PipeliningClient [server port]
 </PRE>
 *
 * <h2>Existing HTTP server</h2>
 *
 * You can also use this client to send a GET request "/hello" to an existing HTTP server (different than
 * {@link PipeliningServer}) by passing the port fo the existing server similar to the case above:
 *
 <PRE>
 java io.reactivex.netty.examples.http.pipelining.PipeliningClient [server port]
 </PRE>
 *
 * In all the above usages, this client will print the response status received from the server.
 */
public class PipeliningClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*
         * Retrieves the server port, using the following algorithm:
         * <ul>
             <li>If an argument is passed, then use the argument as the server port.</li>
             <li>Otherwise, see if the server in the passed server class is already running. If so, use that port.</li>
             <li>Otherwise, start the passed server class and use that port.</li>
         </ul>
         */
        int port = getServerPort(PipeliningServer.class, args);

        /*Since HTTP client does not yet support pipeling, this example uses a TCP client*/
        TcpClient.newClient("localhost", port) /*Create a client*/
                .<FullHttpRequest, FullHttpResponse>pipelineConfigurator(pipeline -> {
                    pipeline.addLast(new HttpClientCodec());
                    pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                })
                .createConnectionRequest() /*Creates a GET request with URI "/hello"*/
                .flatMap(conn -> conn.write(Observable.just(new DefaultFullHttpRequest(HTTP_1_1, GET, "/"),
                                                            new DefaultFullHttpRequest(HTTP_1_1, GET, "/")
                                                           )
                                      )
                                     .ignoreElements()
                                     .cast(FullHttpResponse.class)
                                     .concatWith(conn.getInput())
                )
                .map(resp -> resp.status().toString())
                .take(2)
                  /*Block till the response comes to avoid JVM exit.*/
                .toBlocking()
                .forEach(logger::info);
    }
}
