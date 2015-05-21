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
package io.reactivex.netty.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import javax.net.ssl.SSLException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class SslClientTest {

    @Test(timeout = 60000)
    public void testReleaseOnSslFailure() throws Exception {
        int serverPort = HttpServer.newServer()
                                   .start(new RequestHandler<ByteBuf, ByteBuf>() {
                                       @Override
                                       public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                                      HttpServerResponse<ByteBuf> response) {
                                           return Observable.empty();
                                       }
                                   })
                                   .getServerPort();

        final MaxConnectionsBasedStrategy strategy = new MaxConnectionsBasedStrategy(1);

        final AtomicReference<Channel> channelRef = new AtomicReference<>();

        // The connect fails because the server does not support SSL.
        TestSubscriber<ByteBuf> subscriber = new TestSubscriber<>();
        HttpClient.newClient("127.0.0.1", serverPort)
                  .connectionPoolLimitStrategy(strategy)
                  .unsafeSecure()
                  .createGet("/")
                  .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                      @Override
                      public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
                          channelRef.set(response.unsafeNettyChannel());
                          return response.getContent();
                      }
                  })
                  .subscribe(subscriber);

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0), is(instanceOf(SSLException.class)));
        assertThat("Response channel is null.", channelRef.get(), is(notNullValue()));

        Assert.assertEquals("Unexpected available permits.", 1, strategy.getAvailablePermits());
    }
}
