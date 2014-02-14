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

    public Observable<Void> writeContentAndFlush(final T content) {
        return writeOnChannelAndFlush(content);
    }

    public <R> Observable<Void> writeContentAndFlush(final R content, final ContentTransformer<R> transformer) {
        ByteBuf contentBytes = transformer.transform(content, getAllocator());
        return writeOnChannelAndFlush(contentBytes);
    }

    public Observable<Void> writeContentAndFlush(String content) {
        return writeContentAndFlush(content, new StringTransformer());
    }

    public Observable<Void> writeContentAndFlush(byte[] content) {
        return writeContentAndFlush(content, new ByteTransformer());
    }

    public void writeContent(final T content) {
        writeOnChannel(content);
    }

    public <R> void writeContent(final R content, final ContentTransformer<R> transformer) {
        ByteBuf contentBytes = transformer.transform(content, getAllocator());
        writeOnChannel(contentBytes);
    }

    public void writeContent(String content) {
        writeContent(content, new StringTransformer());
    }

    public void writeContent(byte[] content) {
        writeContent(content, new ByteTransformer());
    }

    public Observable<Void> flush() {
        ctx.flush();
        return unflushedWritesListener.listenForNextCompletion().map(new Func1<ChannelFuture, Void>() {
            @Override
            public Void call(ChannelFuture future) {
                return null;
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
