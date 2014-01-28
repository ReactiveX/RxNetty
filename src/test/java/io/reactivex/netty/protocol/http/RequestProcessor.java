/**
 *
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.reactivex.netty.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.reactivex.netty.ObservableConnection;
import rx.Observable;
import rx.util.functions.Action1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestProcessor implements Action1<ObservableConnection<FullHttpRequest, Object>> {

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
    
    public Observable<Void> handleSingleEntity(ObservableConnection<FullHttpRequest, Object> connection) {
        byte[] responseBytes = "Hello world".getBytes();
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                                                                       connection.channelContext().alloc()
                                                                                 .buffer(responseBytes.length)
                                                                                 .writeBytes(responseBytes));
        return connection.write(response);
    }

    public Observable<Void> handleStreamWithoutChunking(ObservableConnection<FullHttpRequest, Object> connection) {
        ByteBuf content = connection.channelContext().alloc().buffer();
        StringBuilder contentBuilder = new StringBuilder();
        for (String contentPart : smallStreamContent) {
            contentBuilder.append("data:").append(contentPart).append("\n\n");
        }
        content.writeBytes(contentBuilder.toString().getBytes());
        DefaultFullHttpResponse header = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        header.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        return connection.write(header);
    }

    public Observable<Void> handleStream(ObservableConnection<FullHttpRequest, Object> connection) {
        return sendStreamingResponse(connection, smallStreamContent);
    }

    public Observable<Void> handleLargeStream(ObservableConnection<FullHttpRequest, Object> connection) {
        return sendStreamingResponse(connection, largeStreamContent);
    }

    public Observable<Void> simulateTimeout(FullHttpRequest httpRequest,
                                            final ObservableConnection<FullHttpRequest, Object> connection) {
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
        ByteBuf content = connection.channelContext().alloc().buffer(contentBytes.length)
                                    .writeBytes(contentBytes);
        return connection.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content));
    }

    private static Observable<Void> sendStreamingResponse(ObservableConnection<FullHttpRequest, Object> connection,
                                                          List<String> data) {
        HttpResponse header = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        header.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        header.headers().add(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");
        connection.write(header);
        for (String line : data) {
            byte[] contentBytes = ("data:" + line + "\n\n").getBytes();
            connection.write(new DefaultHttpContent(connection.channelContext().alloc().buffer(contentBytes.length)
                                                              .writeBytes(contentBytes)));
        }

        return connection.write(new DefaultLastHttpContent());
    }

    @Override
    public void call(final ObservableConnection<FullHttpRequest, Object> connection) {
        connection.getInput().subscribe(new Action1<FullHttpRequest>() {
            @Override
            public void call(FullHttpRequest httpRequest) {
                String uri = httpRequest.getUri();
                if (uri.startsWith("test/singleEntity")) {
                    handleSingleEntity(connection);
                } else if (uri.startsWith("test/stream")) {
                    handleStream(connection);
                } else if (uri.startsWith("test/nochunk_stream")) {
                    handleStreamWithoutChunking(connection);
                } else if (uri.startsWith("test/largeStream")) {
                    handleLargeStream(connection);
                } else if (uri.startsWith("test/timeout")) {
                    simulateTimeout(httpRequest, connection);
                } else {
                    connection.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
                }
            }
        });
    }
}
