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

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class EventListenerTest {

    @Rule
    public final HttpServerRule rule = new HttpServerRule();

    @Test(timeout = 60000)
    public void testEventListener() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(rule.serverAddress);

        assertListenerCalled(client);
    }

    @Test(timeout = 60000)
    public void testEventListenerPostCopy() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(rule.serverAddress)
                                                        .enableWireLogging("test", LogLevel.ERROR);

        assertListenerCalled(client);
    }

    @Test(timeout = 60000)
    public void testSubscriptionPreCopy() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(rule.serverAddress);

        MockHttpClientEventsListener listener = subscribe(client);

        client = client.enableWireLogging("test", LogLevel.DEBUG);

        connectAndAssertListenerInvocation(client, listener);
    }

    private static void assertListenerCalled(HttpClient<ByteBuf, ByteBuf> client) {
        MockHttpClientEventsListener listener = subscribe(client);
        connectAndAssertListenerInvocation(client, listener);
    }

    private static void connectAndAssertListenerInvocation(HttpClient<ByteBuf, ByteBuf> client,
                                                           MockHttpClientEventsListener listener) {
        TestSubscriber<ByteBuf> subscriber = new TestSubscriber<>();
        client.createGet("")
              .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                  @Override
                  public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> r) {
                      return r.getContent();
                  }
              })
              .take(1)
              .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("HTTP methods not invoked on the listener.", listener.httpListenerInvoked, is(true));
        assertThat("TCP methods not invoked on the listener.", listener.tcpListenerInvoked, is(true));
    }

    private static MockHttpClientEventsListener subscribe(HttpClient<ByteBuf, ByteBuf> client) {
        MockHttpClientEventsListener listener = new MockHttpClientEventsListener();
        client.subscribe(listener);
        return listener;
    }

    public static class HttpServerRule extends ExternalResource {

        private SocketAddress serverAddress;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    serverAddress = HttpServer.newServer().enableWireLogging("test", LogLevel.ERROR)
                                              .start(new RequestHandler<ByteBuf, ByteBuf>() {
                                                  @Override
                                                  public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                                                                 HttpServerResponse<ByteBuf> response) {
                                                      return response.writeString(Observable.just("Hello"));
                                                  }
                                              }).getServerAddress();
                    base.evaluate();
                }
            };
        }
    }

    private static class MockHttpClientEventsListener extends HttpClientEventsListener {

        private volatile boolean httpListenerInvoked;
        private volatile boolean tcpListenerInvoked;

        @Override
        public void onResponseHeadersReceived(int responseCode, long duration, TimeUnit timeUnit) {
            httpListenerInvoked = true;
        }

        @Override
        public void onByteRead(long bytesRead) {
            tcpListenerInvoked = true;
        }
    }

}
