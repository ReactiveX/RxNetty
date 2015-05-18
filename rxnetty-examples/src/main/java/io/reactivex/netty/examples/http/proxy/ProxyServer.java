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

import java.util.Iterator;
import java.util.Map.Entry;

import static rx.Observable.*;

public final class ProxyServer extends AbstractServerExample {

    public static void main(final String[] args) {

        int targetServerPort = startTargetServer();

        final HttpClient<ByteBuf, ByteBuf> targetClient = HttpClient.newClient("localhost", targetServerPort);

        HttpServer<ByteBuf, ByteBuf> server;

        server = HttpServer.newServer(0)
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
                                                       Iterator<Entry<String, String>> respH = outResp.headerIterator();
                                                       while (respH.hasNext()) {
                                                           Entry<String, String> next = respH.next();
                                                           resp.setHeader(next.getKey(), next.getValue());
                                                       }

                                                       resp.setHeader("X-Proxied-By", "RxNetty");
                                                       return resp.write(outResp.getContent());
                                                   });
                                  }
                           );

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
                         .start((req, resp) -> resp.writeString(just("HelloWorld!")))
                         .getServerPort();
    }
}
