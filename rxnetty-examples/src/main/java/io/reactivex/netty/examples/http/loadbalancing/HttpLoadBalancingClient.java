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

package io.reactivex.netty.examples.http.loadbalancing;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.loadbalancer.LoadBalancerFactory;
import io.reactivex.netty.examples.AbstractClientExample;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.loadbalancer.EWMABasedP2CStrategy;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

import java.net.SocketAddress;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * This example demonstrates how to integrate any arbitrary load balancing logic with a {@link HttpClient}. Load
 * balancing algorithms are not provided by {@code RxNetty}, what is provided is a low level construct of
 * {@link ConnectionProvider} that abstracts providing connections for a {@link HttpClient}. Higher level constructs like
 * Load Balancing, connection pooling, etc. can be built using these building blocks.
 *
 * The code here uses a naive {@link HttpLoadBalancer} that removes a host on any connection failure and otherwise round
 * robins on the set of available hosts.
 *
 * This example, starts a couple emebedded HTTP servers two always sending 200 responses and one always sending 503
 * responses to demonstrate failure detection (not using the server that sends 503) and round-robin load balancing
 * (alternating between the two available hosts for the requests)
 *
 * @see ConnectionProvider Low level abstraction to create varied load balancing schemes.
 * @see HttpLoadBalancer An example of load balancer used by this example.
 */
public final class HttpLoadBalancingClient extends AbstractClientExample {

    public static void main(String[] args) {

        /*Start 3 embedded servers, two healthy and one unhealthy to demo failure detection*/
        final Observable<Host> hosts = Observable.just(startNewServer(OK), startNewServer(OK),
                                                       startNewServer(SERVICE_UNAVAILABLE))
                                                 .map(Host::new);

        HttpClient.newClient(LoadBalancerFactory.create(new EWMABasedP2CStrategy<>()), hosts)
                .enableWireLogging(LogLevel.DEBUG)
                .createGet("/hello")
                .doOnNext(resp -> logger.info(resp.toString()))
                .flatMap((HttpClientResponse<ByteBuf> resp) ->
                                 resp.getContent().map(bb -> bb.toString(Charset.defaultCharset()))
                )
                .repeat(5)
                .toBlocking()
                .forEach(logger::info);
    }

    private static SocketAddress startNewServer(HttpResponseStatus cannedStatus) {
        /*Start a new server on an ephemeral port that sends a response with the canned HTTP status for each request.*/
        return HttpServer.newServer()
                         .start((req, resp) -> resp.setStatus(cannedStatus))
                         .getServerAddress();
    }
}
