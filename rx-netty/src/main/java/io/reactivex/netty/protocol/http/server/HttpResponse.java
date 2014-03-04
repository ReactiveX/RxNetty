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
package io.reactivex.netty.protocol.http.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.reactivex.netty.channel.DefaultChannelWriter;
import rx.Observable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Nitesh Kant
 */
public class HttpResponse<T> extends DefaultChannelWriter<T> {

    private final HttpResponseHeaders headers;
    private final io.netty.handler.codec.http.HttpResponse nettyResponse;
    private final AtomicBoolean headerWritten = new AtomicBoolean();
    private ChannelFuture headerWriteFuture;

    public HttpResponse(ChannelHandlerContext ctx) {
        this(ctx, HttpVersion.HTTP_1_1);
    }

    public HttpResponse(ChannelHandlerContext ctx, HttpVersion httpVersion) {
        this(ctx, new DefaultHttpResponse(httpVersion, HttpResponseStatus.OK));
    }

    /*Visible for testing */ HttpResponse(ChannelHandlerContext ctx,
                                          io.netty.handler.codec.http.HttpResponse nettyResponse) {
        super(ctx);
        this.nettyResponse = nettyResponse;
        headers = new HttpResponseHeaders(nettyResponse);
    }

    public HttpResponseHeaders getHeaders() {
        return headers;
    }

    public void addCookie(Cookie cookie) {
        headers.add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
    }

    public void setStatus(HttpResponseStatus status) {
        nettyResponse.setStatus(status);
    }

    public Observable<Void> close() {
        writeOnChannel(new DefaultLastHttpContent());
        return flush();
    }

    io.netty.handler.codec.http.HttpResponse getNettyResponse() {
        return nettyResponse;
    }

    boolean isHeaderWritten() {
        return null != headerWriteFuture && headerWriteFuture.isSuccess();
    }

    @Override
    protected ChannelFuture writeOnChannel(Object msg) {
        if (!HttpResponse.class.isAssignableFrom(msg.getClass()) && headerWritten.compareAndSet(false, true)) {
            headerWriteFuture = super.writeOnChannel(this);
        }

        return super.writeOnChannel(msg);
    }
}
