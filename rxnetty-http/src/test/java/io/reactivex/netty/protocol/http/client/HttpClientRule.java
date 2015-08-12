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
package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.ConnectionFactory;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.PooledConnectionProviderImpl;
import io.reactivex.netty.protocol.http.client.internal.HttpEventPublisherFactory;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.test.util.EmbeddedChannelWithFeeder;
import io.reactivex.netty.test.util.EmbeddedConnectionFactory;
import io.reactivex.netty.test.util.EmbeddedConnectionProvider;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpClientRule extends ExternalResource {

    private HttpClient<ByteBuf, ByteBuf> httpClient;
    private EmbeddedConnectionProvider<ByteBuf, ByteBuf> connFactory;
    private EmbeddedConnectionFactory<ByteBuf, ByteBuf> embeddedChannelBootstrap;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                setup(ConnectionProvider
                        .create(new Func1<ConnectionFactory<ByteBuf, ByteBuf>, ConnectionProvider<ByteBuf, ByteBuf>>() {
                            @Override
                            public ConnectionProvider<ByteBuf, ByteBuf> call(ConnectionFactory<ByteBuf, ByteBuf> cf) {
                                connFactory = new EmbeddedConnectionProvider<>(cf, new HttpEventPublisherFactory());
                                return connFactory;
                            }
                        }));
                base.evaluate();
            }
        };
    }

    public void setupPooledConnectionFactroy(final PoolConfig<ByteBuf, ByteBuf> pConfig) {
        ConnectionProvider<ByteBuf, ByteBuf> factory =
                ConnectionProvider
                        .create(new Func1<ConnectionFactory<ByteBuf, ByteBuf>, ConnectionProvider<ByteBuf, ByteBuf>>() {
                            @Override
                            public ConnectionProvider<ByteBuf, ByteBuf> call(ConnectionFactory<ByteBuf, ByteBuf> cf) {
                                embeddedChannelBootstrap =
                                        new EmbeddedConnectionFactory<>(cf, new HttpEventPublisherFactory());
                                return new PooledConnectionProviderImpl<>(embeddedChannelBootstrap, pConfig,
                                                                          new InetSocketAddress("127.0.0.1", 0));
                            }
                        });
        TcpClient<ByteBuf, ByteBuf> tcpClient = TcpClient.newClient(factory).enableWireLogging(LogLevel.ERROR);
        httpClient = HttpClientImpl.unsafeCreate(tcpClient, new HttpEventPublisherFactory());
    }

    private void setup(ConnectionProvider<ByteBuf, ByteBuf> rawFactory) {
        TcpClient<ByteBuf, ByteBuf> tcpClient = TcpClient.newClient(rawFactory).enableWireLogging(LogLevel.ERROR);
        httpClient = HttpClientImpl.unsafeCreate(tcpClient, new HttpEventPublisherFactory());
    }

    public HttpClient<ByteBuf, ByteBuf> getHttpClient() {
        return httpClient;
    }

    public EmbeddedChannel getLastCreatedChannel() {
        return getLastCreatedChannelWithFeeder().getChannel();
    }

    public EmbeddedChannelWithFeeder getLastCreatedChannelWithFeeder() {
        List<EmbeddedChannelWithFeeder> createdChannels = getCreatedChannels();
        return createdChannels.get(createdChannels.size() - 1);
    }

    public List<EmbeddedChannelWithFeeder> getCreatedChannels() {
        return null == embeddedChannelBootstrap ? connFactory.getCreatedChannels()
                : embeddedChannelBootstrap.getCreatedChannels();
    }
    public TestSubscriber<HttpClientResponse<ByteBuf>> sendRequest(HttpClientRequest<ByteBuf, ByteBuf> request) {
        TestSubscriber<HttpClientResponse<ByteBuf>> testSubscriber = new TestSubscriber<>();
        request.subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        return testSubscriber;
    }

    public TestSubscriber<String> sendRequestAndGetContent(Observable<HttpClientResponse<ByteBuf>> request) {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        request.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent()
                               .map(new Func1<ByteBuf, String>() {
                                   @Override
                                   public String call(ByteBuf byteBuf) {
                                       return byteBuf.toString(Charset.defaultCharset());
                                   }
                               });
            }
        }).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        return testSubscriber;
    }

    public TestSubscriber<Void> sendRequestAndDiscardResponseContent(HttpClientRequest<ByteBuf, ByteBuf> request) {
        TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

        request.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
            @Override
            public Observable<Void> call(HttpClientResponse<ByteBuf> clientResponse) {
                return clientResponse.discardContent();
            }
        }).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        return testSubscriber;
    }

    public TestSubscriber<Void> discardResponseContent(HttpClientResponse<ByteBuf> response) {

        TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

        response.discardContent().subscribe(testSubscriber);
        return testSubscriber;
    }

    public HttpClientResponse<ByteBuf> discardResponseContent(TestSubscriber<HttpClientResponse<ByteBuf>> responseSub) {

        responseSub.awaitTerminalEvent();
        responseSub.assertTerminalEvent();
        responseSub.assertNoErrors();

        HttpClientResponse<ByteBuf> resp = responseSub.getOnNextEvents().get(0);

        TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

        resp.discardContent().subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertTerminalEvent();
        testSubscriber.assertNoErrors();

        return resp;
    }

    public void feedResponse(HttpContent... content) {
        for (HttpContent httpContent : content) {
            getLastCreatedChannelWithFeeder().getFeeder().addToTheFeed(httpContent);
        }
    }

    public void feedResponse(HttpResponse response, HttpContent content) {
        getLastCreatedChannelWithFeeder().getFeeder().addToTheFeed(response, content);
    }

    public void feedResponseHeaders(HttpResponse response, EmbeddedChannelWithFeeder channelWithFeeder) {
        channelWithFeeder.getFeeder().addToTheFeed(response);
    }

    public void feedResponseHeaders(HttpResponse response) {
        feedResponseHeaders(response, getLastCreatedChannelWithFeeder());
    }

    public void feedResponseAndComplete(String... content) {
        feedResponseHeaders(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK),
                            getLastCreatedChannelWithFeeder());
        for (String contentStr : content) {
            ByteBuf contentBuf = Unpooled.buffer().writeBytes(contentStr.getBytes());
            feedResponse(new DefaultHttpContent(contentBuf));
        }

        feedResponse(new DefaultLastHttpContent());
    }

    public void feedResponseAndComplete(HttpResponse response, HttpContent content) {
        feedResponseAndComplete(response, content, getLastCreatedChannelWithFeeder());
    }

    public void feedResponseAndComplete(HttpResponse response, HttpContent content,
                                        EmbeddedChannelWithFeeder channelWithFeeder) {
        channelWithFeeder.getFeeder().addToTheFeed(response, content, new DefaultLastHttpContent());
    }

    public void feedResponseAndComplete(HttpResponse response) {
        feedResponseAndComplete(response, getLastCreatedChannelWithFeeder());
    }

    public void feedResponseAndComplete(HttpResponse response, EmbeddedChannelWithFeeder channelWithFeeder) {
        channelWithFeeder.getFeeder().addToTheFeed(response, new DefaultLastHttpContent());
    }

    public void assertRequestHeadersWritten(HttpMethod method, String uri) {

        Object outbound = getLastCreatedChannel().readOutbound();

        assertThat("Request not written.", outbound, is(notNullValue()));

        if (outbound instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) outbound;
            assertThat("Unexpected HTTP method for the written request.", request.method(), equalTo(method));
            assertThat("Unexpected HTTP method for the written request.", request.uri(), equalTo(uri));
        } else {
            // Read next
            assertRequestHeadersWritten(method, uri);
        }
    }

    public void assertContentWritten(String contentStr) {
        Object outbound = getLastCreatedChannel().readOutbound();

        assertThat("Content not written.", outbound, is(notNullValue()));
        assertThat("Unxpected content.", outbound, is(instanceOf(ByteBuf.class)));

        ByteBuf content = (ByteBuf) outbound;

        assertThat("Unxpected content.", content.toString(Charset.defaultCharset()), equalTo(contentStr));
    }
}
