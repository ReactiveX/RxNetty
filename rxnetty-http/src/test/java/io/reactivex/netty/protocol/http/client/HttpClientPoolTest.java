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
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.client.pool.FIFOIdleConnectionsHolder;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.PooledConnection;
import io.reactivex.netty.protocol.http.client.internal.HttpClientResponseImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.observers.TestSubscriber;

import java.nio.channels.ClosedChannelException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpClientPoolTest {

    @Rule
    public final PooledHttpClientRule clientRule = new PooledHttpClientRule();

    @Test(timeout = 60000)
    public void testBasicAcquireRelease() throws Exception {

        clientRule.assertIdleConnections(0);

        final HttpClientRequest<ByteBuf, ByteBuf> request1 = clientRule.getHttpClient().createGet("/");
        TestSubscriber<Void> subscriber = clientRule.sendRequestAndDiscardResponseContent(request1);

        clientRule.assertIdleConnections(0); // No idle connections post connect
        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        clientRule.feedResponseAndComplete();

        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();

        clientRule.getLastCreatedChannel().runPendingTasks();

        clientRule.assertIdleConnections(1);
    }

    @Test(timeout = 60000)
    public void testBasicAcquireReleaseWithServerClose() throws Exception {

        clientRule.assertIdleConnections(0);

        final HttpClientRequest<ByteBuf, ByteBuf> request1 = clientRule.getHttpClient().createGet("/");
        TestSubscriber<Void> subscriber = clientRule.sendRequestAndDiscardResponseContent(request1);

        clientRule.assertIdleConnections(0); // No idle connections post connect
        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        clientRule.getLastCreatedChannel().close().await();

        subscriber.assertTerminalEvent();
        assertThat("On complete sent instead of onError", subscriber.getOnCompletedEvents(), is(empty()));
        assertThat("Unexpected error notifications count.", subscriber.getOnErrorEvents(), hasSize(1));
        assertThat("Unexpected error notification.", subscriber.getOnErrorEvents().get(0),
                   is(instanceOf(ClosedChannelException.class)));

        clientRule.getLastCreatedChannel().runPendingTasks();

        clientRule.assertIdleConnections(0); // Since, channel is closed, it should be discarded.
    }

    @Test(timeout = 60000)
    public void testCloseOnKeepAliveTimeout() throws Exception {

        clientRule.assertIdleConnections(0);

        final HttpClientRequest<ByteBuf, ByteBuf> request1 = clientRule.getHttpClient().createGet("/");

        TestSubscriber<HttpClientResponse<ByteBuf>> responseSub = clientRule.sendRequest(request1);

        clientRule.assertIdleConnections(0); // No idle connections post connect
        clientRule.assertRequestHeadersWritten(HttpMethod.GET, "/");

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpClientResponseImpl.KEEP_ALIVE_HEADER_NAME,
                               HttpClientResponseImpl.KEEP_ALIVE_TIMEOUT_HEADER_ATTR + "=0");
        clientRule.feedResponseAndComplete(response);

        HttpClientResponse<ByteBuf> resp = clientRule.discardResponseContent(responseSub);
        Channel nettyChannel = resp.unsafeNettyChannel();

        clientRule.getLastCreatedChannel().runPendingTasks();

        // Close is while release, so this should be post running pending tasks
        assertThat("Channel not closed.", nettyChannel.isOpen(), is(false));
        clientRule.assertIdleConnections(0); // Since, the channel is closed
    }

    @Test(/*timeout = 60000*/)
    public void testReuse() throws Exception {
        clientRule.assertIdleConnections(0);

        Channel channel1 = clientRule.sendRequestAndGetChannel();

        clientRule.getLastCreatedChannel().runPendingTasks();

        clientRule.assertIdleConnections(1);

        Channel channel2 = clientRule.sendRequestAndGetChannel();

        assertThat("Connection was not reused.", channel2, is(channel1));
    }

    public static class PooledHttpClientRule extends HttpClientRule {

        private FIFOIdleConnectionsHolder<ByteBuf, ByteBuf> idleConnHolder;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    idleConnHolder = new FIFOIdleConnectionsHolder<>();
                    PoolConfig<ByteBuf, ByteBuf> pConfig = new PoolConfig<>();
                    pConfig.idleConnectionsHolder(idleConnHolder);

                    setupPooledConnectionFactory(pConfig); // sets the client et al.

                    base.evaluate();
                }
            };
        }

        public void assertIdleConnections(int expectedCount) {
            TestSubscriber<PooledConnection<ByteBuf, ByteBuf>> testSub = new TestSubscriber<>();
            idleConnHolder.peek().subscribe(testSub);

            testSub.assertTerminalEvent();
            testSub.assertNoErrors();

            assertThat("Unexpected number of connections in the holder.", testSub.getOnNextEvents(),
                       hasSize(expectedCount));
        }

        protected Channel sendRequestAndGetChannel() {
            final HttpClientRequest<ByteBuf, ByteBuf> request1 = getHttpClient().createGet("/");

            TestSubscriber<HttpClientResponse<ByteBuf>> respSub = sendRequest(request1);

            feedResponseHeaders(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK),
                                getLastCreatedChannelWithFeeder());

            respSub.awaitTerminalEvent();

            assertIdleConnections(0); // No idle connections post connect
            assertRequestHeadersWritten(HttpMethod.GET, "/");

            feedResponse(new DefaultLastHttpContent());

            final HttpClientResponse<ByteBuf> response = discardResponseContent(respSub);

            return response.unsafeNettyChannel();
        }
    }
}
