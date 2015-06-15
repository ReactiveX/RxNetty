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

package io.reactivex.netty.examples.http.loadbalancing;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;

import java.net.SocketAddress;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * An example to demonstrate how to do load balancing with HTTP clients
 */
public final class HttpLoadBalancingClient extends AbstractClientExample {

    public static void main(String[] args) {

        final SocketAddress[] hosts = { startNewServer(OK), startNewServer(SERVICE_UNAVAILABLE)};

        HttpClient.newClient(HttpLoadBalancer.create(hosts)) /*Create a client*/
                .createGet("/hello") /*Creates a GET request with URI "/hello"*/
                .doOnNext(resp -> logger.info(resp.toString()))/*Prints the response headers*/
                .flatMap((HttpClientResponse<ByteBuf> resp) -> /*Return the stream to response content stream.*/
                                     /*Now use the content stream.*/
                                 resp.getContent()
                                     /*Convert ByteBuf to string for each content*/
                                         .map(bb -> bb.toString(Charset.defaultCharset()))
                )
                .repeat(5)
                /*Block till the response comes to avoid JVM exit.*/
                .toBlocking()
                /*Print each content chunk*/
                .forEach(logger::info);
    }

    private static SocketAddress startNewServer(HttpResponseStatus cannedStatus) {
        return HttpServer.newServer()
                         .start((req, resp) -> resp.setStatus(cannedStatus))
                         .getServerAddress();
    }
}
