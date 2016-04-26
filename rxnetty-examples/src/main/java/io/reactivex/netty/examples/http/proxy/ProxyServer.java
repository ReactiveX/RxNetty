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

package io.reactivex.netty.examples.http.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.server.HttpServer;

import java.util.Iterator;
import java.util.Map.Entry;

import static rx.Observable.*;

/**
 * An example to demonstrate how to write a simple HTTP proxy.
 *
 * The intent here is <em>NOT</em> to prescribe how to write a fully functional proxy, which would otherwise require
 * setting appropriate request headers, do appropriate routing on the origin endpoints, etc. Instead, it is to
 * demonstrate how to write a server that forwards the received request, as is to another server using an RxNetty
 * client.
 *
 * This example starts an embedded target server, which is a simple HTTP server that returns a response with HTTP
 * status of 200 and a content of "Hello World!" for every request it recieves. The proxy server then forwards all
 * received requests to this target server preserving all the request and response headers as well as content. It
 * adds an additional response header "X-Proxied-By" with a value "RxNetty" to demonstrate that the response is a
 * proxied response.
 */
public final class ProxyServer {

    public static void main(final String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(ProxyServer.class);

        /*Starts an embedded target server using an ephemeral port.*/
        int targetServerPort = startTargetServer();

        /*Create a new HTTP client pointing to the target server.*/
        final HttpClient<ByteBuf, ByteBuf> targetClient = HttpClient.newClient("127.0.0.1", targetServerPort);

        HttpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port which acts as a proxy to the target server started above.*/
        server = HttpServer.newServer()
                           .enableWireLogging(LogLevel.DEBUG)
                           .start((serverReq, serverResp) -> {
                                      /*
                                       * Create a new HTTP request for the target server, using the method and URI from
                                       * the server request.
                                       */
                                      HttpClientRequest<ByteBuf, ByteBuf> clientReq =
                                              targetClient.createRequest(serverReq.getHttpMethod(), serverReq.getUri());

                                      /*Copy all server request headers to the client request*/
                                      Iterator<Entry<CharSequence, CharSequence>> serverReqHeaders = serverReq.headerIterator();
                                      while (serverReqHeaders.hasNext()) {
                                          Entry<CharSequence, CharSequence> next = serverReqHeaders.next();
                                          /*Since, the client request is copied for each mutation,
                                          use the latest instance*/
                                          clientReq = clientReq.setHeader(next.getKey(), next.getValue());
                                      }

                                      return clientReq.writeContent(serverReq.getContent())
                                              .flatMap(clientResp -> {
                                                  /*Iterator for the client response headers.*/
                                                  Iterator<Entry<CharSequence, CharSequence>> clientRespHeaders =
                                                          clientResp.headerIterator();

                                                  /*Copy all client response headers to the server response.*/
                                                  while (clientRespHeaders.hasNext()) {
                                                      Entry<CharSequence, CharSequence> next = clientRespHeaders.next();
                                                      serverResp.setHeader(next.getKey(), next.getValue());
                                                  }

                                                  /*Add a demo header to indicate proxied response!*/
                                                  serverResp.setHeader("X-Proxied-By", "RxNetty");

                                                  /*Write the client response content to server response.*/
                                                  return serverResp.write(clientResp.getContent());
                                              });
                                  }
                           );

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (env.shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        env.registerServerAddress(server.getServerAddress());
    }

    private static int startTargetServer() {
        /*Start a hello world server using an ephemeral port*/
        return HttpServer.newServer()
                         .start((req, resp) -> resp.writeString(just("HelloWorld!")))
                         .getServerPort();
    }
}
