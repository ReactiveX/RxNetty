/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.clientNew;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ContentTransformer;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class HttpClientRequestImpl<I, O> extends HttpClientRequest<I, O> {

    protected HttpClientRequestImpl(HttpMethod httpMethod, String uri, String defaultHost, int defaultPort) {
        super(new OnSubscribe<HttpClientResponse<O>>() {
            @Override
            public void call(Subscriber<? super HttpClientResponse<O>> subscriber) {
            }
        });
    }

    @Override
    public HttpClientRequest<I, O> setContentSource(Observable<I> contentSource) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <S> HttpClientRequest<I, O> setRawContentSource(Observable<S> rawContentSource,
                                                           ContentTransformer<S> transformer) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <S> HttpClientRequest<I, O> setRawContent(S content, ContentTransformer<S> transformer) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setContent(I content) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setStringContent(String content) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setBytesContent(byte[] content) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> followRedirects(int maxRedirects) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> followRedirects(boolean follow) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> addHeader(CharSequence name, Object value) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> addCookie(Cookie cookie) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> addDateHeader(CharSequence name, Date value) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> addDateHeader(CharSequence name, Iterable<Date> values) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> addHeader(CharSequence name, Iterable<Object> values) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setDateHeader(CharSequence name, Date value) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setHeader(CharSequence name, Object value) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setDateHeader(CharSequence name, Iterable<Date> values) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setHeader(CharSequence name, Iterable<Object> values) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setKeepAlive(boolean keepAlive) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> setTransferEncodingChunked() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerFirst(String name, ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                     ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerLast(String name, ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                    ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                      ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                      String name, ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                                     ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                     String name, ChannelHandler handler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClientRequest<II, OO> withPipelineConfigurator(Action1<ChannelPipeline> configurator) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        // TODO: Auto-generated method stub
        return false;
    }

    @Override
    public HttpClientRequestUpdater<I, O> newUpdater() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public boolean containsHeaderWithValue(CharSequence name, CharSequence value, boolean caseInsensitiveValueMatch) {
        // TODO: Auto-generated method stub
        return false;
    }

    @Override
    public String getHeader(CharSequence name) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAllHeaders(CharSequence name) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpVersion getHttpVersion() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpMethod getMethod() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public String getUri() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public String getAbsoluteUri() {
        // TODO: Auto-generated method stub
        return null;
    }
}
