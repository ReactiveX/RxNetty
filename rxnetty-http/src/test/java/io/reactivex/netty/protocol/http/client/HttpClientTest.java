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
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRule;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func0;
import rx.observers.TestSubscriber;

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

    @Test(timeout = 60000)
    public void testRequestWithNoContentLengthHeaderOrContentReturnsEmptyBody() {
        clientRule.sendRequest(clientRule.getHttpClient().createGet("/"));
        clientRule.assertEmptyBodyWithContentLengthZero();
    }

    @Test(timeout = 60000)
    public void testRequestWithNoContentLengthHeaderAndContentReturnsContentChunkAndSingleEmptyChunk() {
        clientRule.sendRequest(clientRule.getHttpClient().createGet("/")
                .writeStringContent(Observable.just("Hello")));
        clientRule.assertChunks("Hello");
    }

    @Test(timeout = 60000)
    public void testRequestWithContentLengthReturnsRawBody() {
        clientRule.sendRequest(clientRule.getHttpClient().createGet("/")
                .setHeader(HttpHeaderNames.CONTENT_LENGTH, 5)
                .writeStringContent(Observable.just("Hello")));
        clientRule.assertBodyWithContentLength(5, "Hello");
    }

    @Test(timeout = 60000)
    public void testRequestWithZeroContentLengthReturnsEmptyBody() {
        clientRule.sendRequest(clientRule.getHttpClient().createGet("/").setHeader(HttpHeaderNames.CONTENT_LENGTH, 0));
        clientRule.assertEmptyBodyWithContentLengthZero();
    }

    @Test(timeout = 60000)
    public void testRequestWithOnlyPositiveContentLengthReturnsEmptyBody() {
        clientRule.sendRequest(clientRule.getHttpClient().createGet("/").setHeader(HttpHeaderNames.CONTENT_LENGTH, 5));
        clientRule.assertEmptyBodyWithContentLengthZero();
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
