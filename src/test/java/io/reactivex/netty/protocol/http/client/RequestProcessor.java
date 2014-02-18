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
package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.reactivex.netty.protocol.http.server.HttpRequest;
import io.reactivex.netty.protocol.http.server.HttpResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestProcessor implements RequestHandler<ByteBuf, ByteBuf> {

    public static final List<String> smallStreamContent;

    public static final List<String> largeStreamContent;

    static {

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
    
    public Observable<Void> handleSingleEntity(HttpResponse<ByteBuf> response) {
        byte[] responseBytes = "Hello world".getBytes();
        return response.writeBytesAndFlush(responseBytes);
    }

    public Observable<Void> handleStreamWithoutChunking(HttpResponse<ByteBuf> response) {
        response.getHeaders().add(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        for (String contentPart : smallStreamContent) {
            response.writeString("data:");
            response.writeString(contentPart);
            response.writeString("\n\n");
        }
        return response.flush();
    }

    public Observable<Void> handleStream(HttpResponse<ByteBuf> response) {
        return sendStreamingResponse(response, smallStreamContent);
    }

    public Observable<Void> handleLargeStream(HttpResponse<ByteBuf> response) {
        return sendStreamingResponse(response, largeStreamContent);
    }

    public Observable<Void> simulateTimeout(HttpRequest<ByteBuf> httpRequest, HttpResponse<ByteBuf> response) {
        String uri = httpRequest.getUri();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        List<String> timeout = decoder.parameters().get("timeout");
        byte[] contentBytes;
        HttpResponseStatus status = HttpResponseStatus.NO_CONTENT;
        if (null != timeout && !timeout.isEmpty()) {
            try {
                Thread.sleep(Integer.parseInt(timeout.get(0)));
                contentBytes = "".getBytes();
            } catch (Exception e) {
                contentBytes = e.getMessage().getBytes();
                status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            status = HttpResponseStatus.BAD_REQUEST;
            contentBytes = "Please provide a timeout parameter.".getBytes();
        }

        response.setStatus(status);
        return response.writeBytesAndFlush(contentBytes);
    }

    private static Observable<Void> sendStreamingResponse(HttpResponse<ByteBuf> response, List<String> data) {
        response.getHeaders().add(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        response.getHeaders().add(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");
        for (String line : data) {
            byte[] contentBytes = ("data:" + line + "\n\n").getBytes();
            response.writeBytes(contentBytes);
        }

        return response.flush();
    }

    @Override
    public Observable<Void> handle(HttpRequest<ByteBuf> request, HttpResponse<ByteBuf> response) {
        String uri = request.getUri();
        if (uri.startsWith("test/singleEntity")) {
            return handleSingleEntity(response);
        } else if (uri.startsWith("test/stream")) {
            return handleStream(response);
        } else if (uri.startsWith("test/nochunk_stream")) {
            return handleStreamWithoutChunking(response);
        } else if (uri.startsWith("test/largeStream")) {
            return handleLargeStream(response);
        } else if (uri.startsWith("test/timeout")) {
            return simulateTimeout(request, response);
        } else {
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return response.flush();
        }
    }
}
