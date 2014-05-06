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
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import rx.subjects.PublishSubject;

/**
 * A channel handler for {@link HttpServer} to convert netty's http request/response objects to {@link HttpServer}'s
 * request/response objects. It handles the following message types:
 *
 * <h2>Reading Objects</h2>
 * <ul>
 <li>{@link HttpRequest}: Converts it to {@link HttpServerRequest } </li>
 <li>{@link HttpContent}: Converts it to the content of the previously generated
{@link HttpServerRequest }</li>
 <li>{@link FullHttpRequest}: Converts it to a {@link HttpServerRequest } with pre-populated content observable.</li>
 <li>Any other object: Assumes that it is a transformed HTTP content & pass it through to the content observable.</li>
 </ul>
 *
 * <h2>Writing Objects</h2>
 * <ul>
 <li>{@link HttpServerResponse}: Converts it to a {@link HttpResponse}</li>
 <li>{@link ByteBuf} to an {@link HttpContent}</li>
 <li>Pass through any other message type.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public class ServerRequestResponseConverter extends ChannelDuplexHandler {

    @SuppressWarnings("rawtypes") private final PublishSubject contentSubject; // The type of this subject can change at runtime because a user can convert the content at runtime.
    private boolean keepAlive;

    public ServerRequestResponseConverter() {
        contentSubject = PublishSubject.create();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        if (HttpRequest.class.isAssignableFrom(recievedMsgClass)) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            HttpServerRequest rxRequest = new HttpServerRequest((HttpRequest) msg, contentSubject);
            keepAlive = rxRequest.getHeaders().isKeepAlive();
            super.channelRead(ctx, rxRequest); // For FullHttpRequest, this assumes that after this call returns,
                                               // someone has subscribed to the content observable, if not the content will be lost.
        }

        if (HttpContent.class.isAssignableFrom(recievedMsgClass)) {// This will be executed if the incoming message is a FullHttpRequest or only HttpContent.
            ByteBuf content = ((ByteBufHolder) msg).content();
            invokeContentOnNext(content);
            if (LastHttpContent.class.isAssignableFrom(recievedMsgClass)) {
                contentSubject.onCompleted();
            }
        } else {
            invokeContentOnNext(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        if (HttpServerResponse.class.isAssignableFrom(recievedMsgClass)) {
            @SuppressWarnings("rawtypes")
            HttpServerResponse rxResponse = (HttpServerResponse) msg;
            if (keepAlive && !rxResponse.getHeaders().contains(HttpHeaders.Names.CONTENT_LENGTH)) {
                // If there is no content length & it is a keep alive connection. We need to specify the transfer
                // encoding as chunked as we always send data in multiple HttpContent.
                // On the other hand, if someone wants to not have chunked encoding, adding content-length will work
                // as expected.
                rxResponse.getHeaders().add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
            }
            super.write(ctx, rxResponse.getNettyResponse(), promise);
        } else if (ByteBuf.class.isAssignableFrom(recievedMsgClass)) {
            HttpContent content = new DefaultHttpContent((ByteBuf) msg);
            super.write(ctx, content, promise);
        } else {
            super.write(ctx, msg, promise); // pass through, since we do not understand this message.
        }

    }

    @SuppressWarnings("unchecked")
    private void invokeContentOnNext(Object nextObject) {
        try {
            contentSubject.onNext(nextObject);
        } catch (ClassCastException e) {
            contentSubject.onError(e);
        }
    }
}
