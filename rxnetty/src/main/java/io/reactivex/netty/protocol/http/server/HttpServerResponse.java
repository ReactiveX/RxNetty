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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.reactivex.netty.channel.DefaultChannelWriter;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.server.ServerChannelMetricEventProvider;
import io.reactivex.netty.server.ServerMetricsEvent;
import rx.Observable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Nitesh Kant
 */
public class HttpServerResponse<T> extends DefaultChannelWriter<T> {

    private final HttpResponseHeaders headers;
    private final HttpResponse nettyResponse;
    private final AtomicBoolean headerWritten = new AtomicBoolean();
    private volatile boolean fullResponseWritten;
    private ChannelFuture headerWriteFuture;
    private volatile boolean flushOnlyOnReadComplete;

    protected HttpServerResponse(Channel nettyChannel,
                                 MetricEventsSubject<? extends ServerMetricsEvent<?>> eventsSubject) {
        this(nettyChannel, HttpVersion.HTTP_1_1, eventsSubject);
    }

    protected HttpServerResponse(Channel nettyChannel, HttpVersion httpVersion,
                                 MetricEventsSubject<? extends ServerMetricsEvent<?>> eventsSubject) {
        this(nettyChannel, new DefaultHttpResponse(httpVersion, HttpResponseStatus.OK), eventsSubject);
    }

    /*Visible for testing */ HttpServerResponse(Channel nettyChannel, HttpResponse nettyResponse,
                                                MetricEventsSubject<? extends ServerMetricsEvent<?>> eventsSubject) {
        super(nettyChannel, eventsSubject, ServerChannelMetricEventProvider.INSTANCE);
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

    public HttpResponseStatus getStatus() {
        return nettyResponse.getStatus();
    }

    @Override
    public Observable<Void> close() {
        return close(true);
    }

    /**
     * Closes this response with optionally flushing the writes. <br/>
     *
     * <b>Unless it is required by the usecase, it is generally more optimal to leave the decision of when to flush to
     * the framework as that enables a gathering write on the underlying socket, which is more optimal.</b>
     *
     * @param flush If this close should also flush the writes.
     *
     * @return Observable representing the close result.
     */
    @Override
    public Observable<Void> close(boolean flush) {
        return super.close(flush);
    }

    @Override
    public Observable<Void> _close(boolean flush) {

        writeHeadersIfNotWritten();

        if (!fullResponseWritten && (headers.isTransferEncodingChunked() || headers.isKeepAlive())) {
            writeOnChannel(new DefaultLastHttpContent()); // This indicates end of response for netty. If this is not
            // sent for keep-alive connections, netty's HTTP codec will not know that the response has ended and hence
            // will ignore the subsequent HTTP header writes. See issue: https://github.com/Netflix/RxNetty/issues/130
        }
        return flush ? flush() : Observable.<Void>empty();
    }

    public void writeChunkedInput(HttpChunkedInput httpChunkedInput) {
        writeOnChannel(httpChunkedInput);
    }

    /**
     * Flush semantics of a response are as follows:
     * <ul>
        <li>Flush immediately if {@link HttpServerResponse#flush()} is called.</li>
        <li>Flush at the completion of {@link Observable} returned by
     {@link RequestHandler#handle(HttpServerRequest, HttpServerResponse)} if and only if
     {@link #flushOnlyOnChannelReadComplete(boolean)} is set to false (default is false).</li>
        <li>Flush when {@link ChannelHandlerContext#fireChannelReadComplete()} event is fired by netty. This is done
     unconditionally and is a no-op if there is nothing to flush.</li>
     </ul>
     */
    public void flushOnlyOnChannelReadComplete(boolean flushOnlyOnReadComplete) {
        this.flushOnlyOnReadComplete = flushOnlyOnReadComplete;
    }

    public boolean isFlushOnlyOnReadComplete() {
        return flushOnlyOnReadComplete;
    }

    HttpResponse getNettyResponse() {
        return nettyResponse;
    }

    boolean isHeaderWritten() {
        return null != headerWriteFuture && headerWriteFuture.isSuccess();
    }

    @Override
    protected ChannelFuture writeOnChannel(Object msg) {
        /**
         * The following code either sends a single FullHttpResponse or assures that the headers are written before
         * writing any content.
         *
         * A single FullHttpResponse will be written, if and only if,
         * -- The passed object (to be written) is a ByteBuf instance and it's readable bytes are equal to the
         * content-length header value set.
         * -- There is no content ever to be written (content length header is set to zero).
         *
         * We resort to writing a FullHttpResponse in above scenarios to reduce the overhead of write (executing
         * netty's pipeline)
         */
        if (!HttpServerResponse.class.isAssignableFrom(msg.getClass())) {
            if (msg instanceof ByteBuf) {
                ByteBuf content = (ByteBuf) msg;
                long contentLength = headers.getContentLength(-1);
                if (-1 != contentLength && contentLength == content.readableBytes()) {
                    if (headerWritten.compareAndSet(false, true)) {
                        // The passed object (to be written) is a ByteBuf instance and it's readable bytes are equal to the
                        // content-length header value set.
                        // So write full response instead of header, content & last HTTP content.
                        return writeFullResponse((ByteBuf) msg);
                    }
                }
            }
            writeHeadersIfNotWritten();
        } else {
            long contentLength = headers.getContentLength(-1);
            if (0 == contentLength) {
                if (headerWritten.compareAndSet(false, true)) {
                    // There is no content ever to be written (content length header is set to zero).
                    // So write full response instead of header & last HTTP content.
                    return writeFullResponse((ByteBuf) msg);
                }
            }
            // There is no reason to call writeHeadersIfNotWritten() as this is the call to actually write the headers.
        }

        return super.writeOnChannel(msg); // Write the message as is if we did not write FullHttpResponse.
    }

    private ChannelFuture writeFullResponse(ByteBuf content) {
        fullResponseWritten = true;
        FullHttpResponse fhr = new DelegatingFullHttpResponse(nettyResponse, content);
        return super.writeOnChannel(fhr);
    }

    protected void writeHeadersIfNotWritten() {
        if (headerWritten.compareAndSet(false, true)) {
            /**
             * This assertion whether the transfer encoding should be chunked or not, should be done here and not
             * anywhere in the netty's pipeline. The reason is that in close() method we determine whether to write
             * the LastHttpContent based on whether the transfer encoding is chunked or not.
             * Now, if we do this determination & updation of transfer encoding in a handler in the pipeline, it may be
             * that the handler is invoked asynchronously (i.e. when this method is not invoked from the server's
             * eventloop). In such a scenario there will be a race-condition between close() asserting that the transfer
             * encoding is chunked and the handler adding the same and thus in some cases, the LastHttpContent will not
             * be written with transfer-encoding chunked and the response will never finish.
             */
            if (!headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
                // If there is no content length we need to specify the transfer encoding as chunked as we always send
                // data in multiple HttpContent.
                // On the other hand, if someone wants to not have chunked encoding, adding content-length will work
                // as expected.
                headers.add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
            }
            headerWriteFuture = super.writeOnChannel(this);
        }
    }

    /**
     * An implementation of {@link FullHttpResponse} which can be composed of already created headers and content
     * separately. The implementation provided by netty does not provide a way to do this.
     */
    private static class DelegatingFullHttpResponse implements FullHttpResponse {

        private final HttpResponse headers;
        private final ByteBuf content;
        private final HttpHeaders trailingHeaders;

        public DelegatingFullHttpResponse(HttpResponse headers, ByteBuf content) {
            this.headers = headers;
            this.content = content;
            trailingHeaders = new DefaultHttpHeaders(false);
        }

        public static FullHttpResponse newWithNoContent(HttpResponse headers, ByteBufAllocator allocator) {
            headers.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
            return new DelegatingFullHttpResponse(headers, allocator.buffer(0));
        }

        @Override
        public FullHttpResponse copy() {
            DefaultFullHttpResponse copy = new DefaultFullHttpResponse(getProtocolVersion(), getStatus(), content.copy());
            copy.headers().set(headers());
            copy.trailingHeaders().set(trailingHeaders());
            return copy;
        }

        @Override
        public HttpContent duplicate() {
            DefaultFullHttpResponse dup = new DefaultFullHttpResponse(getProtocolVersion(), getStatus(),
                                                                      content.duplicate());
            dup.headers().set(headers());
            dup.trailingHeaders().set(trailingHeaders());
            return dup;
        }

        @Override
        public FullHttpResponse retain(int increment) {
            content.retain(increment);
            return this;
        }

        @Override
        public FullHttpResponse retain() {
            content.retain();
            return this;
        }

        @Override
        public FullHttpResponse setProtocolVersion(HttpVersion version) {
            headers.setProtocolVersion(version);
            return this;
        }

        @Override
        public FullHttpResponse setStatus(HttpResponseStatus status) {
            headers.setStatus(status);
            return this;
        }

        @Override
        public ByteBuf content() {
            return content;
        }

        @Override
        public HttpResponseStatus getStatus() {
            return headers.getStatus();
        }

        @Override
        public HttpVersion getProtocolVersion() {
            return headers.getProtocolVersion();
        }

        @Override
        public HttpHeaders headers() {
            return headers.headers();
        }

        @Override
        public HttpHeaders trailingHeaders() {
            return trailingHeaders;
        }

        @Override
        public DecoderResult getDecoderResult() {
            return DecoderResult.SUCCESS;
        }

        @Override
        public void setDecoderResult(DecoderResult result) {
            // No op as we use this only for write.
        }

        @Override
        public int refCnt() {
            return content.refCnt();
        }

        @Override
        public boolean release() {
            return content.release();
        }

        @Override
        public boolean release(int decrement) {
            return content.release(decrement);
        }
    }
}
