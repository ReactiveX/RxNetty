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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRule;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpClientTest {

    @Rule
    public final HttpClientRule clientRule = new HttpClientRule();

    @Rule
    public final HttpServerRule serverRule = new HttpServerRule();

    @Test(timeout = 60000)
    public void testCloseOnResponseComplete() throws Exception {

        HttpClientRequest<ByteBuf, ByteBuf> request = clientRule.getHttpClient().createGet("/");

        TestSubscriber<Void> testSubscriber = clientRule.sendRequestAndDiscardResponseContent(request);

        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        clientRule.feedResponseAndComplete(nettyResponse);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        assertThat("Channel not closed after response completion.", clientRule.getLastCreatedChannel().isOpen(), is(false));
    }

    @Test(timeout = 60000)
    public void testResponseContent() throws Exception {

        HttpClientRequest<ByteBuf, ByteBuf> request = clientRule.getHttpClient().createGet("/");

        TestSubscriber<String> testSubscriber = clientRule.sendRequestAndGetContent(request);

        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        final String content = "Hello";
        clientRule.feedResponseAndComplete(content);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        assertThat("Unexpected response content count.", testSubscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected response content.", testSubscriber.getOnNextEvents(), contains(content));
    }

    @Test(timeout = 60000)
    public void testResponseContentMultipleChunks() throws Exception {

        HttpClientRequest<ByteBuf, ByteBuf> request = clientRule.getHttpClient().createGet("/");

        TestSubscriber<String> testSubscriber = clientRule.sendRequestAndGetContent(request);

        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        final String content1 = "Hello1";
        final String content2 = "Hello2";
        clientRule.feedResponseAndComplete(content1, content2);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        assertThat("Unexpected response content count.", testSubscriber.getOnNextEvents(), hasSize(2));
        assertThat("Unexpected response content.", testSubscriber.getOnNextEvents(), contains(content1, content2));
    }

    @Test(timeout = 60000)
    public void testAggregatedContent() throws Exception {

        HttpClientRequest<ByteBuf, ByteBuf> request = clientRule.getHttpClient()
                                                                .<ByteBuf, ByteBuf>addChannelHandlerLast("aggregator", new Func0<ChannelHandler>() {
                                                                    @Override
                                                                    public ChannelHandler call() {
                                                                        return new HttpObjectAggregator(1024);
                                                                    }
                                                                })
                                                                .createGet("/");

        TestSubscriber<String> testSubscriber = clientRule.sendRequestAndGetContent(request);

        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        final String content1 = "Hello1";
        final String content2 = "Hello2";
        clientRule.feedResponseAndComplete(content1, content2);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        assertThat("Unexpected response content count.", testSubscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected response content.", testSubscriber.getOnNextEvents().get(0),
                   containsString(content1));
        assertThat("Unexpected response content.", testSubscriber.getOnNextEvents().get(0),
                   containsString(content2));
    }

    @Test(timeout = 60000)
    public void testNoContentSubscribe() throws Exception {
        HttpClientRequest<ByteBuf, ByteBuf> request = clientRule.getHttpClient().createGet("/");

        TestSubscriber<HttpClientResponse<ByteBuf>> testSubscriber = clientRule.sendRequest(request);
        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        clientRule.feedResponseHeaders(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

        testSubscriber.assertTerminalEvent();
    }

    @Test(timeout = 60000)
    public void testPost() throws Exception {
        String contentStr = "Hello";
        Observable<HttpClientResponse<ByteBuf>> request = clientRule.getHttpClient()
                                                                    .createPost("/")
                                                                    .writeStringContent(Observable.just(contentStr));

        TestSubscriber<String> testSubscriber = clientRule.sendRequestAndGetContent(request);

        clientRule.assertRequestHeadersWritten(HttpMethod.POST, "/");
        clientRule.assertContentWritten(contentStr);

        clientRule.feedResponseAndComplete();

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        assertThat("Unexpected response content count.", testSubscriber.getOnNextEvents(), is(empty()));
    }

    @Test(timeout = 60000)
    public void testReadTimeoutNoPooling() throws Exception {

        startServerThatNeverReplies();

        HttpClientRequest<ByteBuf, ByteBuf> request = HttpClient.newClient(serverRule.getServerAddress())
                                                                .readTimeOut(1, TimeUnit.SECONDS)
                                                                .createGet("/");

        TestSubscriber<Void> testSubscriber = clientRule.sendRequestAndDiscardResponseContent(request);

        testSubscriber.awaitTerminalEvent();

        assertThat("On complete invoked, instead of error.", testSubscriber.getOnCompletedEvents(), is(empty()));
        assertThat("Unexpected onError count.", testSubscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected exception.", testSubscriber.getOnErrorEvents().get(0),
                   is(instanceOf(ReadTimeoutException.class)));
    }

    @Test(timeout = 60000)
    public void testReadTimeoutWithPooling() throws Exception {

        startServerThatNeverReplies();

        HttpClientRequest<ByteBuf, ByteBuf> request =
                HttpClient.newClient(SingleHostPoolingProviderFactory.<ByteBuf, ByteBuf>createUnbounded(),
                                     Observable.just(new Host(serverRule.getServerAddress())))
                          .readTimeOut(1, TimeUnit.SECONDS)
                          .createGet("/");

        TestSubscriber<Void> testSubscriber = clientRule.sendRequestAndDiscardResponseContent(request);

        testSubscriber.awaitTerminalEvent();

        assertThat("On complete invoked, instead of error.", testSubscriber.getOnCompletedEvents(), is(empty()));
        assertThat("Unexpected onError count.", testSubscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected exception.", testSubscriber.getOnErrorEvents().get(0),
                   is(instanceOf(ReadTimeoutException.class)));
    }

    //@Test(timeout = 60000) // TODO: Fix me
    public void testReadTimeoutWithPoolReuse() throws Exception {

        serverRule.startServer(new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(final HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                if (request.getUri().startsWith("/never")) {
                    return response.write(Observable.<ByteBuf>empty()
                                                    .doOnCompleted(new Action0() {
                                                        @Override
                                                        public void call() {
                                                            // To flush the headers.
                                                            response.unsafeNettyChannel().flush();
                                                        }
                                                    }))
                                   .write(Observable.<ByteBuf>never());
                } else {
                    return response.write(Observable.<ByteBuf>empty());
                }
            }
        });

        SocketAddress serverAddress = serverRule.getServerAddress();
        ConnectionProviderFactory<ByteBuf, ByteBuf> cpf = SingleHostPoolingProviderFactory.createUnbounded();

        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(cpf, Observable.just(new Host(serverAddress)));

        // Send a request that will create a connection to be reused later.
        HttpClientRequest<ByteBuf, ByteBuf> request = client.createGet("/");
        TestSubscriber<HttpClientResponse<ByteBuf>> testSubscriber = clientRule.sendRequest(request);
        final Channel channel1 = clientRule.discardResponseContent(testSubscriber).unsafeNettyChannel();

        // Send another request on the same client so that the pool can be reused.
        HttpClientRequest<ByteBuf, ByteBuf> timeoutReq = client.createGet("/never").readTimeOut(5, TimeUnit.SECONDS);
        TestSubscriber<HttpClientResponse<ByteBuf>> timeoutSub = clientRule.sendRequest(timeoutReq);

        timeoutSub.awaitTerminalEvent();
        timeoutSub.assertTerminalEvent();
        timeoutSub.assertNoErrors();

        assertThat("No response recieved.", timeoutSub.getOnNextEvents(), hasSize(1));

        HttpClientResponse<ByteBuf> resp = timeoutSub.getOnNextEvents().get(0);
        Channel channel2 = resp.unsafeNettyChannel();

        assertThat("Connection was not reused", channel2, is(channel1));

        TestSubscriber<ByteBuf> contentSub = new TestSubscriber<>();
        resp.getContent().subscribe(contentSub);
        contentSub.awaitTerminalEvent();
        contentSub.assertTerminalEvent();

        assertThat("On complete invoked, instead of error.", contentSub.getOnCompletedEvents(), is(empty()));
        assertThat("Unexpected onError count.", contentSub.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected exception.", contentSub.getOnErrorEvents().get(0),
                   is(instanceOf(ReadTimeoutException.class)));

    }

    @Test(timeout = 60000)
    public void testRequestWithNoContentLengthHeaderOrContentReturnsEmptyBody() {
        serverRule.startServer();

        serverRule.assertRequestEquals(
            new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                @Override
                public Observable<HttpClientResponse<ByteBuf>> call(HttpClient<ByteBuf, ByteBuf> client) {
                    return client.createGet("/");
                }
            },
            "GET / HTTP/1.1\r\n" +
                "content-length: 0\r\n" +
                "host: " + serverRule.getServerAddress().toString().replaceFirst("^/", "") + "\r\n" +
                "\r\n");
    }

    @Test(timeout = 60000)
    public void testRequestWithNoContentLengthHeaderAndContentReturnsContentChunkAndSingleEmptyChunk() {
        serverRule.startServer();

        serverRule.assertRequestEquals(
            new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                @Override
                public Observable<HttpClientResponse<ByteBuf>> call(HttpClient<ByteBuf, ByteBuf> client) {
                    return client.createGet("/")
                            .writeStringContent(Observable.just("Hello"));
                }
            },
            "GET / HTTP/1.1\r\n" +
                "transfer-encoding: chunked\r\n" +
                "host: " + serverRule.getServerAddress().toString().replaceFirst("^/", "") + "\r\n" +
                "\r\n" +
                "5\r\n" +
                "Hello\r\n" +
                "0\r\n" +
                "\r\n");
    }

    @Test(timeout = 60000)
    public void testRequestWithContentLengthReturnsRawBody() {
        serverRule.startServer();

        serverRule.assertRequestEquals(
            new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                @Override
                public Observable<HttpClientResponse<ByteBuf>> call(HttpClient<ByteBuf, ByteBuf> client) {
                    return client.createGet("/")
                            .setHeader(HttpHeaderNames.CONTENT_LENGTH, 5)
                            .writeStringContent(Observable.just("Hello"));
                }
            },
            "GET / HTTP/1.1\r\n" +
                "content-length: 5\r\n" +
                "host: " + serverRule.getServerAddress().toString().replaceFirst("^/", "") + "\r\n" +
                "\r\n" +
                "Hello");
    }

    @Test(timeout = 60000)
    public void testRequestWithZeroContentLengthReturnsEmptyBody() {
        serverRule.startServer();

        serverRule.assertRequestEquals(
            new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                @Override
                public Observable<HttpClientResponse<ByteBuf>> call(HttpClient<ByteBuf, ByteBuf> client) {
                    return client.createGet("/")
                        .setHeader(HttpHeaderNames.CONTENT_LENGTH, 0);
                }
            },
            "GET / HTTP/1.1\r\n" +
            "content-length: 0\r\n" +
            "host: " + serverRule.getServerAddress().toString().replaceFirst("^/", "") + "\r\n" +
            "\r\n");
    }

    @Test(timeout = 60000)
    public void testRequestWithOnlyPositiveContentLengthReturnsEmptyBody() {
        serverRule.startServer();

        serverRule.assertRequestEquals(
            new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                @Override
                public Observable<HttpClientResponse<ByteBuf>> call(HttpClient<ByteBuf, ByteBuf> client) {
                    return client.createGet("/")
                        .setHeader(HttpHeaderNames.CONTENT_LENGTH, 5);
                }
            },
            "GET / HTTP/1.1\r\n" +
            "content-length: 0\r\n" +
            "host: " + serverRule.getServerAddress().toString().replaceFirst("^/", "") + "\r\n" +
            "\r\n");
    }

    protected void startServerThatNeverReplies() {
        serverRule.startServer(new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return Observable.never();
            }
        });
    }

    @Test(timeout = 60000)
    public void testLargeHeaders() throws Exception {

    }
}
