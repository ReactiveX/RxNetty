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

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestProcessor implements RequestHandler<ByteBuf, ByteBuf> {

    private static final AtomicInteger redirectLoopUniqueIndex = new AtomicInteger();

    public static final List<String> smallStreamContent;

    public static final List<String> largeStreamContent;

    public static final long KEEP_ALIVE_TIMEOUT_SECONDS = 1;

    public static final String ONE_KB_VALUE;

    static {
        final byte[] onekb = new byte[1024];
        Arrays.fill(onekb, (byte) 'c');
        ONE_KB_VALUE = new String(onekb);

        List<String> smallStreamListLocal = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            smallStreamListLocal.add("line " + i);
        }
        smallStreamContent = Collections.unmodifiableList(smallStreamListLocal);

        List<String> largeStreamListLocal = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            largeStreamListLocal.add("line " + i);
        }
        largeStreamContent = Collections.unmodifiableList(largeStreamListLocal);
    }

    public static final String SINGLE_ENTITY_BODY = "Hello world";

    public Observable<Void> handleLargeHeaders(HttpServerResponse<ByteBuf> response) {
        response.getHeaders().add("LargeHeader", ONE_KB_VALUE);
        return response.writeBytesAndFlush(SINGLE_ENTITY_BODY.getBytes());
    }

    public Observable<Void> handleSingleEntity(HttpServerResponse<ByteBuf> response) {
        byte[] responseBytes = SINGLE_ENTITY_BODY.getBytes();
        return response.writeBytesAndFlush(responseBytes);
    }

    public Observable<Void> handleStreamWithoutChunking(HttpServerResponse<ByteBuf> response) {
        response.getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        for (String contentPart : smallStreamContent) {
            response.writeString("data:");
            response.writeString(contentPart);
            response.writeString("\n\n");
        }
        return response.flush();
    }

    public Observable<Void> handleStream(HttpServerResponse<ByteBuf> response) {
        return sendStreamingResponse(response, smallStreamContent);
    }

    public Observable<Void> handleLargeStream(HttpServerResponse<ByteBuf> response) {
        return sendStreamingResponse(response, largeStreamContent);
    }

    public Observable<Void> simulateTimeout(HttpServerRequest<ByteBuf> httpRequest, final HttpServerResponse<ByteBuf> response) {
        String uri = httpRequest.getUri();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        List<String> timeout = decoder.parameters().get("timeout");
        if (null != timeout && !timeout.isEmpty()) {
            // Do not use Thread.sleep() here as that blocks the eventloop and since by default the eventloop is shared,
            // a few of these timeout requests can just cause failures in other tests (if running parallely)
            return Observable.interval(Integer.parseInt(timeout.get(0)), TimeUnit.MILLISECONDS)
                             .flatMap(new Func1<Long, Observable<Void>>() {
                                 @Override
                                 public Observable<Void> call(Long aLong) {
                                     response.setStatus(HttpResponseStatus.OK);
                                     return response.writeStringAndFlush("OK");
                                 }
                             });
        } else {
            response.setStatus(HttpResponseStatus.BAD_REQUEST);
            return response.writeStringAndFlush("Please provide a timeout parameter.");
        }
    }

    public Observable<Void> handlePost(final HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
        return request.getContent().flatMap(new Func1<ByteBuf, Observable<Void>>() {
            @Override
            public Observable<Void> call(ByteBuf byteBuf) {
                return response.writeAndFlush(byteBuf.retain());
            }
        });
    }

    public Observable<Void> handleCloseConnection(final HttpServerResponse<ByteBuf> response) {
        response.getHeaders().set("Connection", "close");
        byte[] responseBytes = "Hello world".getBytes();
        return response.writeBytesAndFlush(responseBytes);
    }
    
    public Observable<Void> handleKeepAliveTimeout(final HttpServerResponse<ByteBuf> response) {
        response.getHeaders().set("Keep-Alive", "timeout=" + KEEP_ALIVE_TIMEOUT_SECONDS);
        byte[] responseBytes = "Hello world".getBytes();
        return response.writeBytesAndFlush(responseBytes);
    }

    public Observable<Void> redirectGet(final HttpServerResponse<ByteBuf> response) {
        response.getHeaders().set("Location", "/test/singleEntity");
        response.setStatus(HttpResponseStatus.MOVED_PERMANENTLY);
        return response.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    public Observable<Void> redirectCustom(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
        boolean isRedirectLoop = request.getUri().contains("redirectLoop");
        int currentCount = getIntParamWithDefault(request, "count", 0);
        int redirectsRequested = getIntParamWithDefault(request, "redirectsRequested", 1);
        String location;
        if (currentCount >= redirectsRequested) {
            location = "/test/singleEntity";
        } else {
            location = "/test/" + (isRedirectLoop ? "redirectLoop"
                                                  : "redirectLimited" + redirectLoopUniqueIndex.incrementAndGet())
                       + "?count=" + (currentCount + 1)
                       + "&redirectsRequested=" + redirectsRequested;
        }

        response.getHeaders().set("Location", location);
        response.setStatus(HttpResponseStatus.MOVED_PERMANENTLY);
        return response.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    public Observable<Void> redirectPost(final HttpServerResponse<ByteBuf> response) {
        response.getHeaders().set("Location", "/test/post");
        response.setStatus(HttpResponseStatus.MOVED_PERMANENTLY);
        return response.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    public Observable<Void> redirectAbsolute(final HttpServerResponse<ByteBuf> response) {
        response.getHeaders().set("Location", "http://localhost/test/post");
        response.setStatus(HttpResponseStatus.MOVED_PERMANENTLY);
        return response.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    private static int getIntParamWithDefault(HttpServerRequest<ByteBuf> request, String parameName, int defaultVal) {
        List<String> paramVal = request.getQueryParameters().get(parameName);
        if (null != paramVal && !paramVal.isEmpty()) {
            return Integer.parseInt(paramVal.get(0));
        } else {
            return defaultVal;
        }
    }

    private static Observable<Void> sendStreamingResponse(HttpServerResponse<ByteBuf> response, List<String> data) {
        response.getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        response.getHeaders().set(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");
        for (String line : data) {
            byte[] contentBytes = ("data:" + line + "\n\n").getBytes();
            response.writeBytes(contentBytes);
        }

        return response.flush();
    }

    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        String uri = request.getUri();
        if (uri.startsWith("/") && uri.length() > 1) {
            uri = uri.substring(1);
        }
        if ("/".equals(uri) || uri.contains("test/singleEntity")) {
            // in case of redirect, uri starts with /test/singleEntity 
            return handleSingleEntity(response);
        } else if (uri.startsWith("test/stream")) {
            return handleStream(response);
        } else if (uri.startsWith("test/nochunk_stream")) {
            return handleStreamWithoutChunking(response);
        } else if (uri.startsWith("test/largeStream")) {
            return handleLargeStream(response);
        } else if (uri.startsWith("test/timeout")) {
            return simulateTimeout(request, response);
        } else if (uri.contains("test/post")) {
            return handlePost(request, response);
        } else if (uri.startsWith("test/closeConnection")) {
            return handleCloseConnection(response);
        } else if (uri.startsWith("test/keepAliveTimeout")) {
            return handleKeepAliveTimeout(response);
        } else if (uri.startsWith("test/redirectAbsolute")) {
            return redirectAbsolute(response);
        } else if (uri.startsWith("test/redirectInfinite")) {
            return redirectCustom(request, response);
        } else if (uri.startsWith("test/redirectLoop")) {
            return redirectCustom(request, response);
        } else if (uri.startsWith("test/redirectLimited")) {
            return redirectCustom(request, response);
        } else if (uri.startsWith("test/redirect") && request.getHttpMethod().equals(HttpMethod.GET)) {
            return redirectGet(response);
        } else if (uri.startsWith("test/redirectPost") && request.getHttpMethod().equals(HttpMethod.POST)) {
            return redirectPost(response);
        } else {
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return response.flush();
        }
    }
}
