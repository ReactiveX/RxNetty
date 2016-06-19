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

package io.reactivex.netty.examples.http.interceptors.transformation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.AllocatingTransformer;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.examples.http.interceptors.simple.InterceptingServer;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerInterceptorChain;
import io.reactivex.netty.protocol.http.server.HttpServerInterceptorChain.TransformingInterceptor;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.util.HttpContentStringLineDecoder;

import java.util.List;

import static java.util.Collections.*;
import static rx.Observable.*;

/**
 * An HTTP server that follows a simple text based, new line delimited message protocol over HTTP. <p>
 *
 * The server would expect an integer to be sent as a request payload and return two integers separated by a new line.
 * <p>
 *
 * This example demonstrates the usage of server side interceptors which do data transformations.
 * For interceptors requiring no data transformation see {@link InterceptingServer}
 *
 */
public final class TransformingInterceptorsServer {

    public static void main(final String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(TransformingInterceptorsServer.class);

        HttpServer<String, ByteBuf> server;

        server = HttpServer.newServer()
                           .<String, ByteBuf>addChannelHandlerLast("line-decoder", HttpContentStringLineDecoder::new)
                           .enableWireLogging("inter-server", LogLevel.DEBUG)
                           .start(HttpServerInterceptorChain.<String, ByteBuf>start()
                                          .<Integer, Integer>nextWithTransform(readWriteInts())
                                          .end(numberIncrementingHandler()));

        /*Wait for shutdown if not called from the client (passed an arg)*/
        if (env.shouldWaitForShutdown(args)) {
            server.awaitShutdown();
        }

        /*If not waiting for shutdown, assign the ephemeral port used to a field so that it can be read and used by
        the caller, if any.*/
        env.registerServerAddress(server.getServerAddress());
    }

    private static RequestHandler<Integer, Integer> numberIncrementingHandler() {
        return (req, resp) -> resp.write(req.getContent()
                                            .flatMap(integer -> just(integer, ++integer)));
    }

    private static TransformingInterceptor<String, ByteBuf, Integer, Integer> readWriteInts() {
        return handler -> (request, response)
                -> handler.handle(request.transformContent(stringToInt()),
                                  response.transformContent(new AllocatingTransformer<Integer, ByteBuf>() {
                                      @Override
                                      public List<ByteBuf> transform(Integer toTransform, ByteBufAllocator allocator) {
                                          String msg = toTransform.toString() + '\n';
                                          return singletonList(allocator.buffer()
                                                                        .writeBytes(msg.getBytes())
                                          );
                                      }
                                  }));
    }

    private static Transformer<String, Integer> stringToInt() {
        return o -> o.filter(s ->  !s.isEmpty()).map(Integer::parseInt);
    }
}
