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
package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
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

import java.util.concurrent.TimeUnit;

import static rx.Observable.*;

public class ServerSentEventEndToEndTest {

    private HttpServer<ByteBuf, ByteBuf> sseServer;

    @After
    public void tearDown() throws Exception {
        if (null != sseServer) {
            sseServer.shutdown();
            sseServer.awaitShutdown(1, TimeUnit.MINUTES);
        }
    }

    @Test(timeout = 60000)
    public void testWriteRawString() throws Exception {
        startServer(new Func1<HttpServerResponse<ServerSentEvent>, Observable<Void>>() {

            @Override
            public Observable<Void> call(HttpServerResponse<ServerSentEvent> response) {
                return response.writeStringAndFlushOnEach(just("data: interval 1\n"));
            }
        });

        receiveAndAssertSingleEvent();
    }

    @Test(timeout = 60000)
    public void testWriteRawBytes() throws Exception {
        startServer(new Func1<HttpServerResponse<ServerSentEvent>, Observable<Void>>() {

            @Override
            public Observable<Void> call(HttpServerResponse<ServerSentEvent> response) {
                return response.writeBytesAndFlushOnEach(just("data: interval 1\n".getBytes()));
            }
        });

        receiveAndAssertSingleEvent();
    }

    @Test(timeout = 60000)
    public void testWriteServerSentEvent() throws Exception {
        startServer(new Func1<HttpServerResponse<ServerSentEvent>, Observable<Void>>() {

            @Override
            public Observable<Void> call(HttpServerResponse<ServerSentEvent> response) {
                return response.writeAndFlushOnEach(just(ServerSentEvent.withData("interval 1")));
            }
        });

        receiveAndAssertSingleEvent();
    }

    protected void receiveAndAssertSingleEvent() {
        ServerSentEvent result = receivesSingleEvent();
        Assert.assertNotNull("Unexpected server sent event received.", result);
        Assert.assertEquals("Unexpected event data.", "interval 1", result.contentAsString());
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

        return HttpClient.newClient("127.0.0.1", sseServer.getServerPort())
                         .createGet("/")
                         .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ServerSentEvent>>() {
                             @Override
                             public Observable<ServerSentEvent> call(HttpClientResponse<ByteBuf> resp) {
                                 return resp.getContentAsServerSentEvents();
                             }
                         });
    }

    private void startServer(final Func1<HttpServerResponse<ServerSentEvent>, Observable<Void>> f) {
        sseServer = HttpServer.newServer()
                              .start(new RequestHandler<ByteBuf, ByteBuf>() {
                                  @Override
                                  public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                                 HttpServerResponse<ByteBuf> response) {
                                      return f.call(response.transformToServerSentEvents());
                                  }
                              });
    }
}
