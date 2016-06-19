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
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.TransformingInterceptor;
import io.reactivex.netty.protocol.http.util.HttpContentStringLineDecoder;
import org.slf4j.Logger;
import rx.Observable;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

/**
 * A client for {@link TransformingInterceptorsServer}, which follows a simple text based, new line delimited
 * message protocol. It sends an integer as the request payload and expects two integers separated by new lines, as the
 * response payload.
 *
 * There are three ways of running this example:
 *
 * <h2>Default</h2>
 *
 * The default way is to just run this class with no arguments, which will start a server
 * ({@link TransformingInterceptorsServer})
 * on an ephemeral port, send an integer to the server and print the response.
 *
 * <h2>After starting {@link TransformingInterceptorsServer}</h2>
 *
 * If you want to see how {@link TransformingInterceptorsServer} work, you can run
 * {@link TransformingInterceptorsServer} by yourself and then
 * pass the port on which the server started to this class as a program argument:
 *
 <PRE>
 java io.reactivex.netty.examples.http.interceptors.transformation.InterceptingClient [server port]
 </PRE>
 *
 * <h2>Existing HTTP server</h2>
 *
 * You can also use this client to send a POST request "/ints" to an existing HTTP server (different than
 * {@link TransformingInterceptorsServer}) by passing the port and host of the existing server similar to the case above::
 *
 <PRE>
 java io.reactivex.netty.examples.http.interceptors.transformation.InterceptingClient [server port] [server host]
 </PRE>
 * If the server host is omitted from the above, it defaults to "127.0.0.1"
 *
 * In all the above usages, this client will print the response received from the server.
 *
 * @see TransformingInterceptorsServer Default server for this client.
 */
public final class InterceptingClient {

    public static void main(String[] args) {

        ExamplesEnvironment env = ExamplesEnvironment.newEnvironment(InterceptingClient.class);
        Logger logger = env.getLogger();

        /*
         * Retrieves the server address, using the following algorithm:
         * <ul>
             <li>If any arguments are passed, then use the first argument as the server port.</li>
             <li>If available, use the second argument as the server host, else default to localhost</li>
             <li>Otherwise, start the passed server class and use that address.</li>
         </ul>
         */
        SocketAddress serverAddress = env.getServerAddress(TransformingInterceptorsServer.class, args);

        HttpClient.newClient(serverAddress)
                .<ByteBuf, String>addChannelHandlerLast("line-decoder", HttpContentStringLineDecoder::new)
                .enableWireLogging("inter-client", LogLevel.DEBUG)
                .intercept()
                .<Integer, Integer>nextWithTransform(readWriteInts())
                .finish()
                .createPost("/ints")
                .writeContent(Observable.just(1))
                .doOnNext(resp -> logger.info(resp.toString()))
                .flatMap(HttpClientResponse::getContent)
                .map(Object::toString)
                .toBlocking()
                .forEach(logger::info);
    }

    private static TransformingInterceptor<ByteBuf, String, Integer, Integer> readWriteInts() {
        return provider -> (version, method, uri) -> provider.createRequest(version, method, uri)
                                                             .transformContent(intToByteBuffer())
                                                             .transformResponseContent(o -> o.filter(s -> !s.isEmpty())
                                                                                             .map(Integer::parseInt));
    }

    private static AllocatingTransformer<Integer, ByteBuf> intToByteBuffer() {
        return new AllocatingTransformer<Integer, ByteBuf>() {
            @Override
            public List<ByteBuf> transform(Integer toTransform, ByteBufAllocator allocator) {
                byte[] toWrite = toTransform.toString().getBytes();
                return Collections.singletonList(allocator.buffer().writeBytes(toWrite));
            }
        };
    }
}
