/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class ServerSentEventEndToEndTest {

    private HttpServer<ByteBuf, ServerSentEvent> sseServer;

    @After
    public void tearDown() throws Exception {
        if (null != sseServer) {
            sseServer.shutdown();
            sseServer.waitTillShutdown();
        }
    }

    @Test
    public void testWriteRawString() throws Exception {
        startServer(new Func2<HttpServerResponse<ServerSentEvent>, Long, Observable<Void>>() {

            @Override
            public Observable<Void> call(HttpServerResponse<ServerSentEvent> response,
                                         Long interval) {
                return response.writeStringAndFlush("data: " + interval + '\n');
            }
        });

        receiveAndAssertSingleEvent();
    }

    @Test
    public void testWriteRawBytes() throws Exception {
        startServer(new Func2<HttpServerResponse<ServerSentEvent>, Long, Observable<Void>>() {

            @Override
            public Observable<Void> call(HttpServerResponse<ServerSentEvent> response,
                                         Long interval) {
                return response.writeBytesAndFlush(("data: " + interval + '\n').getBytes());
            }
        });

        receiveAndAssertSingleEvent();
    }

    @Test
    public void testWriteRawByteBuf() throws Exception {
        startServer(new Func2<HttpServerResponse<ServerSentEvent>, Long, Observable<Void>>() {

            @Override
            public Observable<Void> call(HttpServerResponse<ServerSentEvent> response,
                                         Long interval) {
                return response.writeBytesAndFlush(response.getAllocator().buffer().writeBytes(
                        ("data: " + interval + '\n').getBytes()));
            }
        });

        receiveAndAssertSingleEvent();
    }

    protected void receiveAndAssertSingleEvent() {
        ServerSentEvent result = receivesSingleEvent();
        Assert.assertNotNull("Unexpected server sent event received.", result);
        Assert.assertEquals("Unexpected event data.", "0", result.contentAsString());
        Assert.assertNull("Unexpected event type.", result.getEventType());
        Assert.assertNull("Unexpected event id.", result.getEventId());
        result.release();
    }

    protected ServerSentEvent receivesSingleEvent() {
        return receiveSse().take(1).map(new Func1<ServerSentEvent, ServerSentEvent>() {
            @Override
            public ServerSentEvent call(ServerSentEvent serverSentEvent) {
                serverSentEvent.retain();
                return serverSentEvent;
            }
        }).toBlocking().singleOrDefault(null);
    }

    private Observable<ServerSentEvent> receiveSse() {
        return RxNetty.<ByteBuf, ServerSentEvent>newHttpClientBuilder("localhost",
                                                                      sseServer.getServerPort())
                                           .pipelineConfigurator(PipelineConfigurators.<ByteBuf>clientSseConfigurator())
                                           .enableWireLogging(LogLevel.ERROR)
                                           .build()
                                           .submit(HttpClientRequest.createGet("/"))
                                           .flatMap(
                                                   new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
                                                       @Override
                                                       public Observable<ServerSentEvent> call(
                                                               HttpClientResponse<ServerSentEvent> response) {
                                                           if (response.getStatus().equals(HttpResponseStatus.OK)) {
                                                               return response.getContent();
                                                           } else {
                                                               return Observable.error(new IllegalStateException(
                                                                       "Server response status: " + response.getStatus()
                                                                                                            .code()));
                                                           }
                                                       }
                                                   });
    }

    private void startServer(final Func2<HttpServerResponse<ServerSentEvent>, Long, Observable<Void>> writeEventForInterval) {
        sseServer = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ServerSentEvent>() {

            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                           final HttpServerResponse<ServerSentEvent> response) {
                return Observable.interval(1, TimeUnit.SECONDS)
                                 .flatMap(new Func1<Long, Observable<Void>>() {
                                     @Override
                                     public Observable<Void> call(Long interval) {
                                         return writeEventForInterval.call(response, interval);
                                     }
                                 });
            }
        }).enableWireLogging(LogLevel.ERROR)
                           .pipelineConfigurator(PipelineConfigurators.<ByteBuf>serveSseConfigurator())
                           .build();
        sseServer.start();
    }
}
