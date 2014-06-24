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
    private ChannelFuture headerWriteFuture;

    protected HttpServerResponse(ChannelHandlerContext ctx,
                                 MetricEventsSubject<? extends ServerMetricsEvent<?>> eventsSubject) {
        this(ctx, HttpVersion.HTTP_1_1, eventsSubject);
    }

    protected HttpServerResponse(ChannelHandlerContext ctx, HttpVersion httpVersion,
                                 MetricEventsSubject<? extends ServerMetricsEvent<?>> eventsSubject) {
        this(ctx, new DefaultHttpResponse(httpVersion, HttpResponseStatus.OK), eventsSubject);
    }

    /*Visible for testing */ HttpServerResponse(ChannelHandlerContext ctx, HttpResponse nettyResponse,
                                                MetricEventsSubject<? extends ServerMetricsEvent<?>> eventsSubject) {
        super(ctx, eventsSubject, ServerChannelMetricEventProvider.INSTANCE);
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
    public Observable<Void> _close() {

        writeHeadersIfNotWritten();

        if (headers.isTransferEncodingChunked() || headers.isKeepAlive()) {
            writeOnChannel(new DefaultLastHttpContent()); // This indicates end of response for netty. If this is not
            // sent for keep-alive connections, netty's HTTP codec will not know that the response has ended and hence
            // will ignore the subsequent HTTP header writes. See issue: https://github.com/Netflix/RxNetty/issues/130
        }
        return flush();
    }

    HttpResponse getNettyResponse() {
        return nettyResponse;
    }

    boolean isHeaderWritten() {
        return null != headerWriteFuture && headerWriteFuture.isSuccess();
    }

    @Override
    protected ChannelFuture writeOnChannel(Object msg) {
        if (!HttpServerResponse.class.isAssignableFrom(msg.getClass())) {
            writeHeadersIfNotWritten();
        }

        return super.writeOnChannel(msg);
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
}
