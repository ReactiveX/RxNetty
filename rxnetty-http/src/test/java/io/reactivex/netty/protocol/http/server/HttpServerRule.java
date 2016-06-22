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
 *
 */
package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpServerRule extends ExternalResource {

    public static final String WELCOME_SERVER_MSG = "Welcome!";

    private HttpServer<ByteBuf, ByteBuf> server;
    private HttpClient<ByteBuf, ByteBuf> client;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                server = HttpServer.newServer().enableWireLogging("test", LogLevel.INFO);
                base.evaluate();
            }
        };
    }

    public SocketAddress startServer() {
        server.start(new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                           HttpServerResponse<ByteBuf> response) {
                return response.setHeader(CONTENT_LENGTH, WELCOME_SERVER_MSG.getBytes().length)
                               .writeString(Observable.just(WELCOME_SERVER_MSG));
            }
        });
        client = HttpClient.newClient("127.0.0.1", server.getServerPort());
        return server.getServerAddress();
    }

    public void startServer(RequestHandler<ByteBuf, ByteBuf> handler) {
        server.start(handler);
        client = HttpClient.newClient("127.0.0.1", server.getServerPort());
    }

    public void setupClient(HttpClient<ByteBuf, ByteBuf> client) {
        this.client = client;
    }

    public HttpClientResponse<ByteBuf> sendRequest(Observable<HttpClientResponse<ByteBuf>> request) {
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = new TestSubscriber<>();

        request.subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected response count.", subscriber.getOnNextEvents(), hasSize(1));
        return subscriber.getOnNextEvents().get(0);
    }

    public void assertResponseContent(HttpClientResponse<ByteBuf> response) {
        TestSubscriber<ByteBuf> subscriber = new TestSubscriber<>();

        response.getContent().subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected content items.", subscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected content.", subscriber.getOnNextEvents().get(0).toString(Charset.defaultCharset()),
                   equalTo(WELCOME_SERVER_MSG));
    }

    public SocketAddress getServerAddress() {
        return new InetSocketAddress("127.0.0.1", server.getServerPort());
    }

    public void setServer(HttpServer<ByteBuf, ByteBuf> server) {
        this.server = server;
    }

    public HttpServer<ByteBuf, ByteBuf> getServer() {
        return server;
    }

    public HttpClient<ByteBuf, ByteBuf> getClient() {
        return client;
    }
}
