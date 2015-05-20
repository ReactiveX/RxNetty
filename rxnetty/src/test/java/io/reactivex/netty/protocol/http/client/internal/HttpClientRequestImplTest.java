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
package io.reactivex.netty.protocol.http.client.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.tcp.ConnectionInputSubscriberEvent;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.test.util.FlushSelector;
import io.reactivex.netty.test.util.TcpConnectionRequestMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.TestSubscriber;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.*;

public class HttpClientRequestImplTest {

    @Rule
    public final RequestRule requestRule = new RequestRule();

    @Test
    public void testWriteContent() throws Exception {
        Observable<Object> content = Observable.<Object>just("Hello");
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeContent(content);

        requestRule.assertContentWrite(content, newReq);
    }

    @Test
    public void testWriteContentAndFlushOnEach() throws Exception {
        Observable<Object> content = Observable.<Object>just("Hello");
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeContentAndFlushOnEach(content);

        requestRule.assertContentWriteAndFlushOnEach(content, newReq);
    }

    @Test
    public void testWriteStringContent() throws Exception {
        Observable<String> content = Observable.just("Hello");
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeStringContent(content);
        requestRule.assertContentWrite(content, newReq);
    }

    @Test
    public void testWriteBytesContent() throws Exception {
        Observable<byte[]> content = Observable.just("Hello".getBytes());
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeBytesContent(content);
        requestRule.assertContentWrite(content, newReq);
    }

    @Test
    public void testWriteContentWithFlushSelector() throws Exception {
        Observable<Object> content = Observable.<Object>just("Hello");
        FlushSelector<Object> flushSelector = new FlushSelector<>(5);
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeContent(content, flushSelector);

        requestRule.assertContentWrite(content, newReq, flushSelector);
    }

    @Test
    public void testWriteStringContentWithFlushSelector() throws Exception {
        Observable<String> content = Observable.just("Hello");
        FlushSelector<String> flushSelector = new FlushSelector<>(5);
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeStringContent(content, flushSelector);

        requestRule.assertContentWrite(content, newReq, flushSelector);
    }

    @Test
    public void testWriteBytesContentWithFlushSelector() throws Exception {
        Observable<byte[]> content = Observable.just("Hello".getBytes());
        FlushSelector<byte[]> flushSelector = new FlushSelector<>(5);
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeBytesContent(content, flushSelector);

        requestRule.assertContentWrite(content, newReq, flushSelector);
    }

    @Test
    public void testWriteContentWithTrailer() throws Exception {
        Observable<Object> content = Observable.<Object>just("Hello");
        TestTrailerFactory tFactory = requestRule.newTrailerFactory();
        TestTrailerMutator<Object> tMutator = requestRule.newTrailerMutator();
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeContent(content, tFactory, tMutator);

        requestRule.assertContentWrite(content, newReq, tFactory, tMutator);
    }

    @Test
    public void testWriteStringContentWithTrailer() throws Exception {
        Observable<String> content = Observable.just("Hello");
        TestTrailerFactory tFactory = requestRule.newTrailerFactory();
        TestTrailerMutator<String> tMutator = requestRule.newTrailerMutator();
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeStringContent(content, tFactory,
                                                                                                tMutator);

        requestRule.assertContentWrite(content, newReq, tFactory, tMutator);
    }

    @Test
    public void testWriteBytesContentWithTrailer() throws Exception {
        Observable<byte[]> content = Observable.just("Hello".getBytes());
        TestTrailerFactory tFactory = requestRule.newTrailerFactory();
        TestTrailerMutator<byte[]> tMutator = requestRule.newTrailerMutator();
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeBytesContent(content, tFactory,
                                                                                               tMutator);

        requestRule.assertContentWrite(content, newReq, tFactory, tMutator);
    }

    @Test
    public void testWriteContentWithTrailerAndSelector() throws Exception {
        Observable<Object> content = Observable.<Object>just("Hello".getBytes());
        FlushSelector<Object> selector = new FlushSelector<>(1);
        TestTrailerFactory tFactory = requestRule.newTrailerFactory();
        TestTrailerMutator<Object> tMutator = requestRule.newTrailerMutator();
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeContent(content, tFactory, tMutator,
                                                                                          selector);

        requestRule.assertContentWrite(content, newReq, tFactory, tMutator, 2/*One for content & one for trailer*/);
    }

    @Test
    public void testWriteStringContentWithTrailerAndSelector() throws Exception {
        Observable<String> content = Observable.just("Hello");
        FlushSelector<String> selector = new FlushSelector<>(1);
        TestTrailerFactory tFactory = requestRule.newTrailerFactory();
        TestTrailerMutator<String> tMutator = requestRule.newTrailerMutator();
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeStringContent(content, tFactory,
                                                                                                tMutator, selector);

        requestRule.assertContentWrite(content, newReq, tFactory, tMutator, 2/*One for content & one for trailer*/);
    }

    @Test
    public void testWriteBytesContentWithTrailerAndSelector() throws Exception {
        Observable<byte[]> content = Observable.just("Hello".getBytes());
        FlushSelector<byte[]> selector = new FlushSelector<>(1);
        TestTrailerFactory tFactory = requestRule.newTrailerFactory();
        TestTrailerMutator<byte[]> tMutator = requestRule.newTrailerMutator();
        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeBytesContent(content, tFactory,
                                                                                               tMutator, selector);

        requestRule.assertContentWrite(content, newReq, tFactory, tMutator, 2/*One for content & one for trailer*/);
    }

    @Test
    public void testAddHeader() throws Exception {
        final String headerName = "Foo";
        final String headerVal = "bar";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, headerVal);

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, headerVal);
    }

    @Test
    public void testAddCookie() throws Exception {
        DefaultCookie cookie = new DefaultCookie("cookie", "cook");
        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addCookie(cookie);

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, Names.COOKIE, ClientCookieEncoder.encode(cookie));
    }

    @Test
    public void testAddDateHeader() throws Exception {
        String headerName = "date";
        Date date = new Date();

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addDateHeader(headerName, date);

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, date);
    }

    @Test
    public void testAddDateHeaderMulti() throws Exception {
        String headerName = "date";
        Date date1 = new Date();
        Date date2 = new Date();

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addDateHeader(headerName,
                                                                                           Arrays.asList(date1, date2));

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, date1, date2);
    }

    @Test
    public void testAddDateHeaderIncrementally() throws Exception {
        String headerName = "foo";
        Date date1 = new Date();
        Date date2 = new Date();

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, date1);

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, date1);

        HttpClientRequestImpl<Object, ByteBuf> newReq2 =
                (HttpClientRequestImpl<Object, ByteBuf>) newReq.addHeader(headerName, date2);

        requestRule.assertCopy(newReq, newReq2);

        requestRule.assertHeaderAdded(newReq2, headerName, date1, date2);
    }

    @Test
    public void testAddHeaderMulti() throws Exception {
        String headerName = "foo";
        String val1 = "val1";
        String val2 = "val2";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request
                                                    .addHeaderValues(headerName, Arrays.<Object>asList(val1, val2));

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, val1, val2);
    }

    @Test
    public void testAddHeaderIncrementally() throws Exception {
        String headerName = "foo";
        String val1 = "val1";
        String val2 = "val2";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, val1);

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, val1);

        HttpClientRequestImpl<Object, ByteBuf> newReq2 =
                (HttpClientRequestImpl<Object, ByteBuf>) newReq.addHeader(headerName, val2);

        requestRule.assertCopy(newReq, newReq2);

        requestRule.assertHeaderAdded(newReq2, headerName, val1, val2);
    }

    @Test
    public void testSetDateHeader() throws Exception {
        String headerName = "date";
        Date date1 = new Date();

        HttpClientRequestImpl<Object, ByteBuf> addReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addDateHeader(headerName, date1);

        requestRule.assertCopy(addReq);

        requestRule.assertHeaderAdded(addReq, headerName, date1);

        Date date2 = new Date(100);
        HttpClientRequestImpl<Object, ByteBuf> setReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.setDateHeader(headerName, date2);

        requestRule.assertCopy(setReq);

        requestRule.assertHeaderAdded(setReq, headerName, date2);
    }

    @Test
    public void testSetHeader() throws Exception {
        String headerName = "foo";
        String val1 = "bar";

        HttpClientRequestImpl<Object, ByteBuf> addReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, val1);

        requestRule.assertCopy(addReq);

        requestRule.assertHeaderAdded(addReq, headerName, val1);

        String val2 = "bar2";
        HttpClientRequestImpl<Object, ByteBuf> setReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.setHeader(headerName, val2);

        requestRule.assertCopy(setReq);

        requestRule.assertHeaderAdded(setReq, headerName, val2);
    }

    @Test
    public void testSetDateHeaderMulti() throws Exception {
        String headerName = "date";
        Date date1 = new Date();

        HttpClientRequestImpl<Object, ByteBuf> addReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addDateHeader(headerName, date1);

        requestRule.assertCopy(addReq);

        requestRule.assertHeaderAdded(addReq, headerName, date1);

        Date date2 = new Date(100);
        Date date3 = new Date(500);

        HttpClientRequestImpl<Object, ByteBuf> setReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request
                        .setDateHeader(headerName, Arrays.asList(date2, date3));

        requestRule.assertCopy(setReq);

        requestRule.assertHeaderAdded(setReq, headerName, date2, date3);
    }

    @Test
    public void testSetHeaderMulti() throws Exception {
        String headerName = "date";
        Date date1 = new Date();

        HttpClientRequestImpl<Object, ByteBuf> addReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addDateHeader(headerName, date1);

        requestRule.assertCopy(addReq);

        requestRule.assertHeaderAdded(addReq, headerName, date1);

        String val2 = "bar2";
        String val3 = "bar3";

        HttpClientRequestImpl<Object, ByteBuf> setReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request
                        .setHeaderValues(headerName, Arrays.<Object>asList(val2, val3));

        requestRule.assertCopy(setReq);

        requestRule.assertHeaderAdded(setReq, headerName, val2, val3);
    }

    @Test
    public void testRemoveHeader() throws Exception {
        final String headerName = "Foo";
        final String headerVal = "bar";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, headerVal);

        requestRule.assertCopy(newReq);

        requestRule.assertHeaderAdded(newReq, headerName, headerVal);

        HttpClientRequestImpl<Object, ByteBuf> newReq2 =
                (HttpClientRequestImpl<Object, ByteBuf>) newReq.removeHeader(headerName);

        requestRule.assertCopy(newReq2, newReq);

        HttpRequest newReqHeaders = newReq2.getRawRequest().getHeaders();
        HttpRequest origReqHeaders = newReq.getRawRequest().getHeaders();

        assertThat("Header not removed.", newReqHeaders.headers().contains(headerName), is(false));
        assertThat("Header removed from original request.", origReqHeaders.headers().contains(headerName),
                   is(true));
    }

    @Test
    public void testSetKeepAlive() throws Exception {
        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.setKeepAlive(false);

        requestRule.assertHeaderAdded(newReq, Names.CONNECTION, Values.CLOSE);
    }

    @Test
    public void testSetTransferEncodingChunked() throws Exception {
        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.setTransferEncodingChunked();

        requestRule.assertHeaderAdded(newReq, Names.TRANSFER_ENCODING, Values.CHUNKED);

    }

    @Test
    public void testAddChannelHandlerFirst() throws Exception {
        String handlerName = "handler1";
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerFirst(handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerFirst(handlerName, handlerFactory);
    }

    @Test
    public void testAddChannelHandlerFirstWithGroup() throws Exception {
        String handlerName = "handler1";
        EventExecutorGroup group = new NioEventLoopGroup();
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerFirst(group, handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerFirst(group, handlerName, handlerFactory);
    }

    @Test
    public void testAddChannelHandlerLastWithGroup() throws Exception {
        String handlerName = "handler1";
        EventExecutorGroup group = new NioEventLoopGroup();
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerLast(group, handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerLast(group, handlerName, handlerFactory);
    }

    @Test
    public void testAddChannelHandlerLast() throws Exception {
        String handlerName = "handler1";
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerLast(handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerLast(handlerName, handlerFactory);
    }

    @Test
    public void testAddChannelHandlerBefore() throws Exception {
        String baseName = "base";
        String handlerName = "handler1";
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerBefore(baseName, handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerBefore(baseName, handlerName, handlerFactory);
    }

    @Test
    public void testAddChannelHandlerBeforeWithGroup() throws Exception {
        String baseName = "base";
        String handlerName = "handler1";
        EventExecutorGroup group = new NioEventLoopGroup();
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerBefore(group, baseName, handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerBefore(group, baseName, handlerName, handlerFactory);
    }
    @Test
    public void testAddChannelHandlerAfter() throws Exception {
        String baseName = "base";
        String handlerName = "handler1";
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerAfter(baseName, handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerAfter(baseName, handlerName, handlerFactory);
    }

    @Test
    public void testAddChannelHandlerAfterWithGroup() throws Exception {
        String baseName = "base";
        String handlerName = "handler1";
        EventExecutorGroup group = new NioEventLoopGroup();
        Func0<ChannelHandler> handlerFactory = new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new ChannelDuplexHandler();
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>addChannelHandlerAfter(group, baseName, handlerName, handlerFactory);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).addChannelHandlerAfter(group, baseName, handlerName, handlerFactory);
    }

    @Test
    public void testPipelineConfigurator() throws Exception {
        Action1<ChannelPipeline> configurator = new Action1<ChannelPipeline>() {
            @Override
            public void call(ChannelPipeline pipeline) {
            }
        };

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>pipelineConfigurator(configurator);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).pipelineConfigurator(configurator);

    }

    @Test
    public void testEnableWireLogging() throws Exception {
        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule
                        .request.<Object, ByteBuf>enableWireLogging(LogLevel.DEBUG);

        assertThat("Request not copied.", newReq, not(equalTo(requestRule.request)));

        Mockito.verify(requestRule.clientMock).enableWireLogging(LogLevel.DEBUG);
    }

    @Test
    public void testContainsHeader() throws Exception {
        final String headerName = "Foo";
        final String headerVal = "bar";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, headerVal);

        requestRule.assertHeaderAdded(newReq, headerName, headerVal);

        assertThat("Added header not retrievable.", newReq.containsHeader(headerName), is(true));
    }

    @Test
    public void testContainsHeaderWithValue() throws Exception {
        final String headerName = "Foo";
        final String headerVal1 = "bar";
        final String headerVal2 = "bar2";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request
                        .addHeaderValues(headerName, Arrays.<Object>asList(headerVal1, headerVal2));

        requestRule.assertHeaderAdded(newReq, headerName, headerVal1, headerVal2);

        assertThat("Added header not retrievable.", newReq.containsHeaderWithValue(headerName, headerVal1, false),
                   is(true));
    }

    @Test
    public void testContainsHeaderWithValueCaseInsensitive() throws Exception {
        final String headerName = "Foo";
        final String headerVal = "bar";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, headerVal);

        requestRule.assertHeaderAdded(newReq, headerName, headerVal);

        assertThat("Added header not retrievable.", newReq.containsHeaderWithValue(headerName, "BaR", true),
                   is(true));
    }

    @Test
    public void testGetHeader() throws Exception {
        final String headerName = "Foo";
        final String headerVal = "bar";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request.addHeader(headerName, headerVal);

        requestRule.assertHeaderAdded(newReq, headerName, headerVal);

        assertThat("Added header not retrievable.", newReq.getHeader(headerName), is(headerVal));
    }

    @Test
    public void testGetAllHeaders() throws Exception {
        final String headerName = "Foo";
        final String headerVal1 = "bar";
        final String headerVal2 = "bar2";

        HttpClientRequestImpl<Object, ByteBuf> newReq =
                (HttpClientRequestImpl<Object, ByteBuf>) requestRule.request
                        .addHeaderValues(headerName, Arrays.<Object>asList(headerVal1, headerVal2));

        requestRule.assertHeaderAdded(newReq, headerName, headerVal1, headerVal2);

        assertThat("Added header not retrievable.", newReq.getAllHeaders(headerName),
                   hasSize(2));

        assertThat("Added header not retrievable.", newReq.getAllHeaders(headerName),
                   contains(headerVal1, headerVal2));
    }

    @Test
    public void testGetHttpVersion() throws Exception {
        assertThat("Unexpected http version", requestRule.request.getHttpVersion(), is(HttpVersion.HTTP_1_1));
    }

    @Test
    public void testGetMethod() throws Exception {
        assertThat("Unexpected http version", requestRule.request.getMethod(), is(HttpMethod.GET));
    }

    @Test
    public void testGetUri() throws Exception {
        assertThat("Unexpected http version", requestRule.request.getUri(), is("/"));
    }

    @Test
    public void testSubscribe() throws Exception {
        TestSubscriber<Object> subscriber = new TestSubscriber<>();

        Observable<HttpClientResponse<ByteBuf>> newReq = requestRule.request.writeStringContent(Observable.just("Hello"));
        RawRequest<Object, ByteBuf> rawReq = RequestRule.getRawRequest(newReq);

        newReq.subscribe(subscriber);

        requestRule.channel.flush(); /*Since nobody subscribes to the observable.*/

        assertThat("Unexpected number of items written on the channel.", requestRule.channel.outboundMessages(),
                   hasSize(1));
        Object outboundMsg = requestRule.channel.readOutbound();
        assertThat("Unexpected item written on the channel.", outboundMsg, instanceOf(Observable.class));

        @SuppressWarnings("unchecked")
        Observable<Object> writtenO = (Observable<Object>) outboundMsg;
        TestSubscriber<Object> writtenOSub = new TestSubscriber<>();
        writtenO.subscribe(writtenOSub);

        writtenOSub.assertTerminalEvent();
        writtenOSub.assertNoErrors();

        @SuppressWarnings("unchecked")
        Observable<Object> rawReqO = (Observable<Object>) rawReq.asObservable(requestRule.channel);

        TestSubscriber<Object> rawReqOSub = new TestSubscriber<>();
        rawReqO.subscribe(rawReqOSub);

        rawReqOSub.assertTerminalEvent();
        rawReqOSub.assertNoErrors();

        assertThat("Unexpected items count in Observable written on channel.", writtenOSub.getOnNextEvents(),
                   hasSize(rawReqOSub.getOnNextEvents().size()));
        assertThat("Unexpected items in Observable written on channel.", writtenOSub.getOnNextEvents(),
                   contains(rawReqOSub.getOnNextEvents().toArray()));

        DefaultFullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                                            HttpResponseStatus.ACCEPTED);
        HttpClientResponseImpl<Object> response = new HttpClientResponseImpl<>(nettyResponse, requestRule.channel);
        requestRule.addToConnectionInput(response);

        subscriber.assertTerminalEvent();
        subscriber.assertNoErrors();

        assertThat("Unexpected response count received.", subscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected response received.", subscriber.getOnNextEvents().get(0), is((Object) response));
    }

    public static class RequestRule extends ExternalResource {

        private HttpClientRequestImpl<Object, ByteBuf> request;
        private TcpClient<ByteBuf, HttpClientResponse<ByteBuf>> clientMock;
        private Connection<ByteBuf, HttpClientResponse<ByteBuf>> connMock;
        private EmbeddedChannel channel;
        @SuppressWarnings("rawtypes")
        private Subscriber cis;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    @SuppressWarnings("unchecked")
                    TcpClient<ByteBuf, HttpClientResponse<ByteBuf>> clientMock =
                            (TcpClient<ByteBuf, HttpClientResponse<ByteBuf>>) Mockito.mock(TcpClient.class);

                    channel = new EmbeddedChannel(new ChannelDuplexHandler() {
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            if (evt instanceof ConnectionInputSubscriberEvent) {
                                @SuppressWarnings({"rawtypes", "unchecked"})
                                ConnectionInputSubscriberEvent cise = (ConnectionInputSubscriberEvent) evt;
                                cis = cise.getSubscriber();
                            }
                            super.userEventTriggered(ctx, evt);
                        }
                    });

                    connMock = ConnectionImpl.create(channel, new MetricEventsSubject<>(),
                                                     ClientChannelMetricEventProvider.INSTANCE);

                    @SuppressWarnings("unchecked")
                    final
                    TcpConnectionRequestMock<ByteBuf, HttpClientResponse<ByteBuf>> connReqMock =
                            new TcpConnectionRequestMock(Observable.just(connMock));

                    Mockito.when(clientMock.createConnectionRequest())
                           .thenAnswer(new Answer<Object>() {
                               @Override
                               public Object answer(InvocationOnMock invocation) throws Throwable {
                                   return connReqMock;
                               }
                           });

                    Answer<Object> returnThisMock = new Answer<Object>() {
                        @Override
                        public Object answer(InvocationOnMock invocation) throws Throwable {
                            return invocation.getMock();
                        }
                    };

                    Mockito.when(clientMock.addChannelHandlerFirst(anyString(),
                                                                   Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerFirst(Matchers.<EventExecutorGroup>anyObject(),
                                                                   anyString(),
                                                                   Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerLast(anyString(),
                                                                   Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerLast(Matchers.<EventExecutorGroup>anyObject(),
                                                                   anyString(),
                                                                   Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerBefore(anyString(), anyString(),
                                                                    Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerBefore(Matchers.<EventExecutorGroup>anyObject(),
                                                                    anyString(), anyString(),
                                                                    Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerAfter(anyString(), anyString(),
                                                                   Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.addChannelHandlerAfter(Matchers.<EventExecutorGroup>anyObject(),
                                                                   anyString(), anyString(),
                                                                   Matchers.<Func0<ChannelHandler>>anyObject()))
                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.pipelineConfigurator(Matchers.<Action1<ChannelPipeline>>anyObject()))
                                           .thenAnswer(returnThisMock);

                    Mockito.when(clientMock.enableWireLogging(Matchers.<LogLevel>anyObject()))
                                           .thenAnswer(returnThisMock);

                    RequestRule.this.clientMock = clientMock;

                    request = (HttpClientRequestImpl<Object, ByteBuf>) HttpClientRequestImpl.create(HttpVersion.HTTP_1_1,
                                                                                                    HttpMethod.GET, "/",
                                                                                                    RequestRule.this.clientMock);
                    base.evaluate();
                }
            };
        }

        public void createNewRequest(HttpMethod method, String uri) {
            request = (HttpClientRequestImpl<Object, ByteBuf>) HttpClientRequestImpl.create(HttpVersion.HTTP_1_1,
                                                                                            method, uri, clientMock);
        }

        public void assertCopy(HttpClientRequestImpl<Object, ByteBuf> newReq) {
            assertCopy(request, newReq);
        }

        public void assertCopy(HttpClientRequestImpl<Object, ByteBuf> oldReq,
                               HttpClientRequestImpl<Object, ByteBuf> newReq) {
            assertThat("Request not copied.", newReq, not(equalTo(oldReq)));
            assertThat("Underlying raw request not copied.", newReq.getRawRequest(),
                       not(equalTo(oldReq.getRawRequest())));
            assertThat("Underlying raw request headers not copied.", newReq.getRawRequest().getHeaders(),
                       not(equalTo(oldReq.getRawRequest().getHeaders())));
        }

        public void assertHeaderAdded(HttpClientRequestImpl<Object, ByteBuf> newReq, String headerName,
                                      String... headerVals) {
            assertHeaderAdded(request, newReq, headerName, headerVals);
        }

        public void assertHeaderAdded(HttpClientRequestImpl<Object, ByteBuf> oldReq,
                                      HttpClientRequestImpl<Object, ByteBuf> newReq, String headerName,
                                      String... headerVals) {

            HttpRequest newReqHeaders = newReq.getRawRequest().getHeaders();
            HttpRequest origReqHeaders = oldReq.getRawRequest().getHeaders();

            assertThat("New header not added.", newReqHeaders.headers().contains(headerName), is(true));
            assertThat("Unexpected header value.", newReqHeaders.headers().getAll(headerName), contains(headerVals));
            assertThat("More than one header added.", newReqHeaders.headers().names(), hasSize(1));

            assertThat("New header added to original request.", origReqHeaders.headers().names(), is(empty()));
        }

        public void assertHeaderAdded(HttpClientRequestImpl<Object, ByteBuf> newReq, String headerName,
                                      Date... dates) {
            SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

            String[] expectedValues = new String[dates.length];
            for (int i = 0; i < dates.length; i++) {
                Date date = dates[i];
                expectedValues[i] = sdf.format(date);
            }

            assertHeaderAdded(newReq, headerName, expectedValues);
        }

        RawRequest<Object, ByteBuf> assertContentWrite(@SuppressWarnings("rawtypes") Observable contentWritten,
                                           Observable<HttpClientResponse<ByteBuf>> newReq) {
            RawRequest<Object, ByteBuf> rawRequest = _assertContentWriteContentOnly(contentWritten, newReq);
            assertThat("Unexpected flush selector in the created raw request.", rawRequest.getFlushSelector(),
                       is(nullValue()));
            assertThat("Unexpected trailers flag in the created raw request.", rawRequest.hasTrailers(),
                       is(false));
            return rawRequest;
        }

        RawRequest<Object, ByteBuf> assertContentWriteAndFlushOnEach(@SuppressWarnings("rawtypes") Observable contentWritten,
                                                                      Observable<HttpClientResponse<ByteBuf>> newReq) {

            RawRequest<Object, ByteBuf> rawRequest = _assertContentWriteContentOnly(contentWritten, newReq);
            assertThat("Unexpected flush selector in the created raw request.", rawRequest.getFlushSelector(),
                       is(notNullValue()));
            /*Just a way to assert that it is an unconditional flush on each*/
            assertThat("Unexpected flush selector implementation in the created raw request.",
                       rawRequest.getFlushSelector().call(null), is(true));
            assertThat("Unexpected trailers flag in the created raw request.", rawRequest.hasTrailers(),
                       is(false));
            return rawRequest;
        }

        RawRequest<Object, ByteBuf> assertContentWrite(@SuppressWarnings("rawtypes") Observable contentWritten,
                                           Observable<HttpClientResponse<ByteBuf>> newReq,
                                           @SuppressWarnings("rawtypes") Func1 selector) {

            RawRequest<Object, ByteBuf> rawRequest = _assertContentWriteContentOnly(contentWritten, newReq);

            @SuppressWarnings({"unchecked", "rawtypes"})
            Func1 selectorFound = rawRequest.getFlushSelector();
            assertThat("Unexpected flush selector in the created raw request.", selectorFound,
                       is(notNullValue()));
            assertThat("Unexpected flush selector implementation in the created raw request.",
                       selectorFound, equalTo(selector));
            assertThat("Unexpected trailers flag in the created raw request.", rawRequest.hasTrailers(),
                       is(false));
            return rawRequest;
        }

        public <T> int assertContentWrite(Observable<T> content, Observable<HttpClientResponse<ByteBuf>> newReq,
                                          TestTrailerFactory tFactory, TestTrailerMutator<T> tMutator) {
            RawRequest<Object, ByteBuf> rawReq = getRawRequest(newReq);

            final AtomicInteger flushCount = new AtomicInteger();
            EmbeddedChannel channel = new EmbeddedChannel() {
                @Override
                public Channel flush() {
                    flushCount.incrementAndGet();
                    return super.flush();
                }
            };

            Observable<?> reqAsO = rawReq.asObservable(channel);

            TestSubscriber<T> writtenContentSub = new TestSubscriber<>();
            content.subscribe(writtenContentSub);

            writtenContentSub.assertTerminalEvent();
            writtenContentSub.assertNoErrors();

            TestSubscriber<Object> reqSubscriber = new TestSubscriber<>();
            reqAsO.subscribe(reqSubscriber);

            reqSubscriber.awaitTerminalEvent();
            reqSubscriber.assertNoErrors();

            @SuppressWarnings("unchecked")
            List<Object> writtenOnNextEvents = (List<Object>) writtenContentSub.getOnNextEvents();

            List<Object> reqOnNextEvents = reqSubscriber.getOnNextEvents();

            assertThat("Unexpected items in raw request as Observable.", reqOnNextEvents,
                       hasSize(writtenOnNextEvents.size() + 2));

            assertThat("Unexpected type of first item in raw request Observable.", reqOnNextEvents.get(0),
                       instanceOf(HttpRequest.class));

            HttpRequest headers = (HttpRequest) reqOnNextEvents.get(0);
            assertThat("Unexpected headers in the created raw request.", headers,
                       is(request.getRawRequest().getHeaders()));

            assertThat("Unexpected type of last item in raw request Observable.",
                       reqOnNextEvents.get(reqOnNextEvents.size() - 1),
                       instanceOf(TrailingHeaders.class));

            TrailingHeaders trailers = (TrailingHeaders) reqOnNextEvents.get(reqOnNextEvents.size() - 1);
            assertThat("Unexpected trailing headers in the created raw request.", trailers,
                       is(tFactory.lastReturned));
            assertThat("Unexpected trailer mutator invocation count.", tMutator.callCount,
                       is(writtenOnNextEvents.size()));

            List<Object> contentItems = reqOnNextEvents.subList(1, reqOnNextEvents.size() - 1);

            assertThat("Unexpected content items count in raw request as Observable.", contentItems,
                       hasSize(writtenOnNextEvents.size()));
            assertThat("Unexpected content items in raw request as Observable.", contentItems,
                       contains(writtenOnNextEvents.toArray()));

            return flushCount.get();
        }

        public <T> void assertContentWrite(Observable<T> content, Observable<HttpClientResponse<ByteBuf>> newReq,
                                           TestTrailerFactory tFactory, TestTrailerMutator<T> tMutator,
                                           int expectedFlushCounts) {
            int flushCount = assertContentWrite(content, newReq, tFactory, tMutator);
            assertThat("Unexpected flush counts", flushCount, is(expectedFlushCounts));
        }

        private RawRequest<Object, ByteBuf> _assertContentWriteContentOnly(@SuppressWarnings("rawtypes") Observable contentWritten,
                                                       Observable<HttpClientResponse<ByteBuf>> newReq) {
            RawRequest<Object, ByteBuf> rawRequest = getRawRequest(newReq);

            assertThat("Unexpected headers in the created raw request.", rawRequest.getHeaders(),
                       is(request.getRawRequest().getHeaders()));

            assertThat("Unexpected content in the created raw request.", rawRequest.getContent(), is(contentWritten));
            return rawRequest;
        }

        static RawRequest<Object, ByteBuf> getRawRequest(Observable<HttpClientResponse<ByteBuf>> newReq) {
            assertThat("Unexpected request.", newReq, instanceOf(HttpClientRequestImpl.class));

            HttpClientRequestImpl<Object, ByteBuf> asClientReq = (HttpClientRequestImpl<Object, ByteBuf>) newReq;

            return asClientReq.getRawRequest();
        }

        public TestTrailerFactory newTrailerFactory() {
            return new TestTrailerFactory();
        }

        public <T> TestTrailerMutator<T> newTrailerMutator() {
            return new TestTrailerMutator<>();
        }

        @SuppressWarnings("unchecked")
        public void addToConnectionInput(Object msg) {
            if (null != cis) {
                cis.onNext(msg);
            } else {
                throw new AssertionError("Connection input subscriber not found");
            }
        }
    }

    public static class TestTrailerFactory implements Func0<TrailingHeaders> {

        private volatile TrailingHeaders lastReturned;

        @Override
        public TrailingHeaders call() {
            lastReturned = new TrailingHeaders();
            return lastReturned;
        }
    }

    public static class TestTrailerMutator<T> implements Func2<TrailingHeaders, T, TrailingHeaders> {

        private volatile int callCount;

        @Override
        public TrailingHeaders call(TrailingHeaders trailingHeaders,T content) {
            callCount++;
            return trailingHeaders;
        }
    }
}