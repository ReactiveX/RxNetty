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
package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.channel.pool.PoolConfig;
import io.reactivex.netty.protocol.tcp.client.EmbeddedChannelWithFeeder;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpRedirectTest {

    @Rule
    public final HttpClientRule clientRule = new HttpClientRule();

    @Test(timeout = 60000)
    public void testNoLocation() throws Exception {

        final String requestUri = "/";

        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(requestUri);

        assertRequestWritten(requestUri);
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SEE_OTHER);
        clientRule.feedResponseAndComplete(response);

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(HttpRedirectException.class)));

    }

    @Test(timeout = 60000)
    public void testInvalidRedirectLocation() throws Exception {

        final String requestUri = "/";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(requestUri);

        assertRequestWritten(requestUri);
        sendRedirects(" "); // blank is an invalid URI

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(HttpRedirectException.class)));

    }

    @Test(timeout = 60000)
    public void testTooManyRedirect() throws Throwable {

        final String requestUri = "/";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(requestUri);

        assertRequestWritten(requestUri);
        sendRedirects("/blah", "/blah");

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(HttpRedirectException.class)));
    }

    @Test(timeout = 60000)
    public void testRedirectLoop() throws Throwable {

        final String requestUri = "/blah";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(requestUri);

        assertRequestWritten(requestUri);
        sendRedirects(requestUri);

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(HttpRedirectException.class)));
    }

    @Test(timeout = 60000)
    public void testAbsoluteRedirect() throws Throwable {

        final String requestUri = "/blah";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(requestUri);

        assertRequestWritten(requestUri);
        sendRedirects("http://localhost:8888/blah");

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected onNext notifications count.", subscriber.getOnNextEvents(), hasSize(1));
        HttpClientResponse<ByteBuf> response = subscriber.getOnNextEvents().get(0);
        assertThat("Unexpected response.", response, is(notNullValue()));
        assertThat("Unexpected response status.", response.getStatus().code(), is(HttpResponseStatus.SEE_OTHER.code()));
    }

    @Test(timeout = 60000)
    public void testRedirectNoConnPool() throws Throwable {

        final String requestUri = "/";

        HttpClient<ByteBuf, ByteBuf> client = clientRule.getHttpClient().followRedirects(1);
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(client, requestUri);

        assertRequestWritten(requestUri);
        sendRedirects("/blah", "/blah");

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(HttpRedirectException.class)));
    }

    @Test(timeout = 60000)
    public void testRedirectWithConnPool() throws Throwable {
        PoolConfig<ByteBuf, ByteBuf> pConfig = new PoolConfig<ByteBuf, ByteBuf>().maxConnections(10);

        clientRule.setupPooledConnectionFactroy(pConfig); // sets the client et al.

        HttpClient<ByteBuf, ByteBuf> client = clientRule.getHttpClient().followRedirects(1);

        final String requestUri = "/";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(client, requestUri);
        assertRequestWritten(requestUri);

        sendRedirects("/blah", "blah");

        subscriber.awaitTerminalEvent();

        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(HttpRedirectException.class)));
    }

    @Test(timeout = 60000)
    public void testNoRedirect() {
        HttpClient<ByteBuf, ByteBuf> client = clientRule.getHttpClient().followRedirects(false);

        final String requestUri = "/";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(client, requestUri);

        assertRequestWritten(requestUri);
        sendRedirects("/blah2");

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected onNext notifications count.", subscriber.getOnNextEvents(), hasSize(1));
        HttpClientResponse<ByteBuf> response = subscriber.getOnNextEvents().get(0);
        assertThat("Unexpected response.", response, is(notNullValue()));
        assertThat("Unexpected response status.", response.getStatus().code(), is(HttpResponseStatus.SEE_OTHER.code()));
    }

    @Test(timeout = 60000)
    public void testRedirectPost() throws Throwable {

        final String requestUri = "/";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(HttpMethod.POST, requestUri);

        final HttpResponseStatus responseStatus = HttpResponseStatus.FOUND;

        clientRule.assertRequestHeadersWritten(HttpMethod.POST, requestUri);
        sendRedirects(responseStatus, "/blah");

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected onNext notifications count.", subscriber.getOnNextEvents(), hasSize(1));
        HttpClientResponse<ByteBuf> response = subscriber.getOnNextEvents().get(0);
        assertThat("Unexpected response.", response, is(notNullValue()));
        assertThat("Unexpected response status.", response.getStatus().code(), is(responseStatus.code()));
    }

    @Test(timeout = 60000)
    public void testRedirectPostWith303() throws Throwable {

        final String requestUri = "/";
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = sendRequest(HttpMethod.POST, requestUri);

        clientRule.assertRequestHeadersWritten(HttpMethod.POST, requestUri);
        sendRedirects(HttpResponseStatus.SEE_OTHER, "/blah");

        sendResponse(HttpResponseStatus.OK);

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected onNext notifications count.", subscriber.getOnNextEvents(), hasSize(1));
        HttpClientResponse<ByteBuf> response = subscriber.getOnNextEvents().get(0);
        assertThat("Unexpected response.", response, is(notNullValue()));
        assertThat("Unexpected response status.", response.getStatus().code(), is(HttpResponseStatus.OK.code()));
    }

    private static TestSubscriber<HttpClientResponse<ByteBuf>> sendRequest(HttpClient<ByteBuf, ByteBuf> client,
                                                                           HttpMethod method, String uri) {
        final HttpClientRequest<ByteBuf, ByteBuf> req = client.createRequest(method, uri);
        TestSubscriber<HttpClientResponse<ByteBuf>> subscriber = new TestSubscriber<>();
        req.subscribe(subscriber);
        subscriber.assertNoErrors();
        return subscriber;
    }

    private void assertRequestWritten(String uri) {
        clientRule.assertRequestHeadersWritten(HttpMethod.GET, uri);
    }

    private static TestSubscriber<HttpClientResponse<ByteBuf>> sendRequest(HttpClient<ByteBuf, ByteBuf> client,
                                                                           String uri) {
        return sendRequest(client, HttpMethod.GET, uri);
    }

    private TestSubscriber<HttpClientResponse<ByteBuf>> sendRequest(String uri) {
        return sendRequest(clientRule.getHttpClient().followRedirects(1), uri);
    }

    private TestSubscriber<HttpClientResponse<ByteBuf>> sendRequest(HttpMethod method, String uri) {
        return sendRequest(clientRule.getHttpClient().followRedirects(1), method, uri);
    }

    private void sendRedirects(String... locations) {
        sendRedirects(HttpResponseStatus.SEE_OTHER, locations);
    }

    private void sendRedirects(HttpResponseStatus redirectStatus, String... locations) {

        for (int i = 0; i < locations.length; i++) {
            List<EmbeddedChannelWithFeeder> createdChannels = clientRule.getCreatedChannels();
            assertThat("Not enough channels created by the embedded factory.", createdChannels,
                       hasSize(greaterThanOrEqualTo(i + 1)));
            String location = locations[i];
            EmbeddedChannelWithFeeder channelWithFeeder = createdChannels.get(i);
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, redirectStatus);
            response.headers().set(LOCATION, location);
            clientRule.feedResponseAndComplete(response, channelWithFeeder);
        }
    }

    private void sendResponse(HttpResponseStatus redirectStatus) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, redirectStatus);
        clientRule.feedResponseAndComplete(response);
    }
}
