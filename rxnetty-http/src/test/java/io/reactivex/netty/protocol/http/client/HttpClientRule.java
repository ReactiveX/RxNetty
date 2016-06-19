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
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory;
import io.reactivex.netty.test.util.embedded.EmbeddedChannelProvider;
import io.reactivex.netty.test.util.embedded.EmbeddedChannelWithFeeder;
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

    private EmbeddedChannelProvider channelProvider;
    private HttpClient<ByteBuf, ByteBuf> httpClient;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                channelProvider = new EmbeddedChannelProvider();
                httpClient = HttpClient.newClient(new InetSocketAddress(0))
                                       .enableWireLogging("test", LogLevel.ERROR)
                                       .channelProvider(channelProvider.asFactory());
                base.evaluate();
            }
        };
    }

    public void setupPooledConnectionFactory(final PoolConfig<ByteBuf, ByteBuf> pConfig) {
        channelProvider = new EmbeddedChannelProvider();
        httpClient = HttpClient.newClient(SingleHostPoolingProviderFactory.create(pConfig),
                                          Observable.just(new Host(new InetSocketAddress(0))))
                               .channelProvider(channelProvider.asFactory());
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
        return channelProvider.getCreatedChannels();
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

        boolean found = false;
        Object outbound;
        final String expectedFirstLineStart = method.name().toUpperCase() + ' ' + uri;
        String data = null;

        while ((outbound = getLastCreatedChannel().readOutbound()) != null) {
            if (outbound instanceof ByteBuf) {
                ByteBuf bb = (ByteBuf) outbound;
                data = bb.toString(Charset.defaultCharset());
                if (data.startsWith(expectedFirstLineStart)) {
                    found = true;
                    break;
                }
            }
        }

        assertThat("Unexpected HTTP method & URI for the written request.", data,
                   startsWith(expectedFirstLineStart));

        if (!found) {
            assertThat("Request not written.", outbound, is(notNullValue()));
        }
    }

    public void assertContentWritten(String contentStr) {
        boolean found = false;
        Object outbound;
        String data = null;

        while ((outbound = getLastCreatedChannel().readOutbound()) != null) {
            if (outbound instanceof ByteBuf) {
                ByteBuf bb = (ByteBuf) outbound;
                data = bb.toString(Charset.defaultCharset());
                if (data.equalsIgnoreCase(contentStr)) {
                    found = true;
                    break;
                }
            }
        }

        assertThat("Unexpected HTTP content.", data, equalToIgnoringCase(contentStr));

        if (!found) {
            assertThat("Content not written.", outbound, is(notNullValue()));
        }
    }
}
