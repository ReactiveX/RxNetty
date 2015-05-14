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

package io.reactivex.netty.examples.http.proxy;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.examples.AbstractServerExample;
import io.reactivex.netty.protocol.http.clientNew.HttpClient;
import io.reactivex.netty.protocol.http.clientNew.HttpClientRequest;
import io.reactivex.netty.protocol.http.serverNew.HttpServer;
import io.reactivex.netty.servo.http.HttpClientListener;
import io.reactivex.netty.servo.http.HttpServerListener;
import rx.Observable;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static rx.Observable.*;

public final class ProxyServer extends AbstractServerExample {

    public static void main(final String[] args) {

        int targetServerPort = startTargetServer();

        final HttpClient<ByteBuf, ByteBuf> targetClient = HttpClient.newClient("localhost", targetServerPort)
                                                                    .maxConnections(500);
        final HttpClientListener metricListener = HttpClientListener.newHttpListener("blah");
        targetClient.subscribe(metricListener);

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer(8888)
                           .start((req, resp) -> {
                                      HttpClientRequest<ByteBuf, ByteBuf> outReq =
                                              targetClient.createRequest(req.getHttpMethod(), req.getUri());

                                      Iterator<Entry<String, String>> headers = req.headerIterator();
                                      while (headers.hasNext()) {
                                          Entry<String, String> next = headers.next();
                                          outReq = outReq.setHeader(next.getKey(), next.getValue());
                                      }

                                      return outReq.writeContent(req.getContent())
                                                   .flatMap(outResp -> {
                                                       Iterator<Entry<String, String>> respH = req.headerIterator();
                                                       while (respH.hasNext()) {
                                                           Entry<String, String> next = respH.next();
                                                           resp.setHeader(next.getKey(), next.getValue());
                                                       }
                                                       return resp.sendHeaders().write(outResp.getContent());
                                                   });
                                  }
                           );

        HttpServerListener serverListener = HttpServerListener.newHttpListener("bar");
        server.subscribe(serverListener);

        Observable.interval(10, TimeUnit.SECONDS)
                  .forEach(aLong -> {
                      System.out.println("====================  Client   =====================");
                      System.out.print("Pool acquires => " + metricListener.getPoolAcquires());
                      System.out.print(", Pending pool acquires => " + metricListener.getPendingPoolAcquires());
                      System.out.println(", Pool acquire times => " + metricListener.getPoolAcquireTimes().getValue());
                      System.out.print("Pool releases => " + metricListener.getPoolReleases());
                      System.out.print(", Pending pool releases => " + metricListener.getPendingPoolReleases());
                      System.out.println(", Pool release times  => " + metricListener.getPoolReleaseTimes().getValue());
                      System.out.println("Pool reuse => " + metricListener.getPoolReuse());
                      System.out.print("Connection count => " + metricListener.getConnectionCount());
                      System.out.print(", Live connections => " + metricListener.getLiveConnections());
                      System.out.print(", Pending connects => " + metricListener.getPendingConnects());
                      System.out.print(", Pending connection close => " + metricListener.getPendingConnectionClose());
                      System.out.print(", Processed requests => " + metricListener.getProcessedRequests());
                      System.out.print(", Inflight requests => " + metricListener.getInflightRequests());
                      System.out.println(", Response read times => " + metricListener.getResponseReadTimes().getValue());
                      System.out.println("====================  Server   =====================");
                      System.out.print("Inflight requests => " + serverListener.getInflightRequests());
                      System.out.print(", Connected clients => " + serverListener.getLiveConnections());
                      System.out.print(", Processed requests => " + serverListener.getProcessedRequests());
                      System.out.print(", Failed requests => " + serverListener.getFailedRequests());
                      System.out.print(", Response write failed => " + serverListener.getResponseWriteFailed());
                      System.out.print(", Response write times => " + serverListener.getResponseWriteTimes().getValue());
                      System.out.println(", Request read times => " + serverListener.getRequestReadTimes().getValue());
                  });

        /*Wait for shutdown if not called from another class (passed an arg)*/
        if (shouldWaitForShutdown(args)) {
            /*When testing the args are set, to avoid blocking till shutdown*/
            server.waitTillShutdown();
        }

        /*Assign the ephemeral port used to a field so that it can be read and used by the caller, if any.*/
        serverPort = server.getServerPort();
    }

    private static int startTargetServer() {
        return HttpServer.newServer(0)
                         .start((req, resp) ->
                                        req.discardContent() /*Discard content since we do not read it.*/
                                                .concatWith(resp.sendHeaders()
                                                              /*Write the "Hello World" response*/
                                                                    .writeString(just("HelloWorld!")))
                         ).getServerPort();
    }
}
