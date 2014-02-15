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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.MultipleFutureListener;
import io.reactivex.netty.serialization.ByteTransformer;
import io.reactivex.netty.serialization.ContentTransformer;
import io.reactivex.netty.serialization.StringTransformer;
import rx.Observable;
import rx.util.functions.Func1;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Nitesh Kant
 */
public class HttpResponse<T> {

    private final HttpVersion httpVersion;
    private final HttpResponseHeaders headers;
    private final io.netty.handler.codec.http.HttpResponse nettyResponse;
    private final AtomicBoolean headerWritten = new AtomicBoolean();
    private final ChannelHandlerContext ctx;
    private final MultipleFutureListener unflushedWritesListener;

    public HttpResponse(ChannelHandlerContext ctx) {
        this(ctx, HttpVersion.HTTP_1_1);
    }

    public HttpResponse(ChannelHandlerContext ctx, HttpVersion httpVersion) {
        this.ctx = ctx;
        this.httpVersion = httpVersion;
        nettyResponse = new DefaultHttpResponse(this.httpVersion, HttpResponseStatus.OK);
        headers = new HttpResponseHeaders(nettyResponse);
        unflushedWritesListener = new MultipleFutureListener(ctx);
    }

    public HttpResponseHeaders getHeaders() {
        return headers;
    }

    public void addCookie(@SuppressWarnings("unused") Cookie cookie) {
        //TODO: Cookie handling.
    }

    public void setStatus(HttpResponseStatus status) {
        nettyResponse.setStatus(status);
    }

    public Observable<Void> writeAndFlush(final T content) {
        return writeOnChannelAndFlush(content);
    }

    public <R> Observable<Void> writeAndFlush(final R content, final ContentTransformer<R> transformer) {
        ByteBuf contentBytes = transformer.transform(content, getAllocator());
        return writeOnChannelAndFlush(contentBytes);
    }

    public Observable<Void> writeAndFlush(String content) {
        return writeAndFlush(content, new StringTransformer());
    }

    public Observable<Void> writeAndFlush(byte[] content) {
        return writeAndFlush(content, new ByteTransformer());
    }

    public void write(final T content) {
        writeOnChannel(content);
    }

    public <R> void write(final R content, final ContentTransformer<R> transformer) {
        ByteBuf contentBytes = transformer.transform(content, getAllocator());
        writeOnChannel(contentBytes);
    }

    public void write(String content) {
        write(content, new StringTransformer());
    }

    public void write(byte[] content) {
        write(content, new ByteTransformer());
    }

    public Observable<Void> flush() {
        ctx.flush();
        return unflushedWritesListener.listenForNextCompletion().take(1).flatMap(new Func1<ChannelFuture, Observable<Void>>() {
            @Override
            public Observable<Void> call(ChannelFuture future) {
                return Observable.empty();
            }
        });
    }

    public Observable<Void> close() {
        return writeOnChannelAndFlush(new DefaultLastHttpContent());
    }

    public ByteBufAllocator getAllocator() {
        return ctx.alloc();
    }

    io.netty.handler.codec.http.HttpResponse getNettyResponse() {
        return nettyResponse;
    }

    private ChannelFuture writeOnChannel(Object msg) {
        if (!HttpResponse.class.isAssignableFrom(msg.getClass()) && headerWritten.compareAndSet(false, true)) {
            writeOnChannel(this);
        }
        ChannelFuture writeFuture = ctx.channel().write(msg); // Calling write on context will be wrong as the context will be of a component not necessarily, the head of the pipeline.
        addToUnflushedWrites(writeFuture);
        return writeFuture;
    }

    private void addToUnflushedWrites(ChannelFuture writeFuture) {
        unflushedWritesListener.listen(writeFuture);
    }

    private Observable<Void> writeOnChannelAndFlush(Object msg) {
        writeOnChannel(msg);
        return flush();
    }
}
