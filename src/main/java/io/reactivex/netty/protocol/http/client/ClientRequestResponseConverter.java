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
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.protocol.http.MultipleFutureListener;
import io.reactivex.netty.serialization.ContentTransformer;
import rx.Observer;
import rx.subjects.PublishSubject;

/**
 * A channel handler for {@link HttpClient} to convert netty's http request/response objects to {@link HttpClient}'s
 * request/response objects. It handles the following message types:
 *
 * <h2>Reading Objects</h2>
 * <ul>
 <li>{@link io.netty.handler.codec.http.HttpResponse: Converts it to {@link HttpResponse} </li>
 <li>{@link HttpContent}: Converts it to the content of the previously generated
{@link HttpResponse}</li>
 <li>{@link FullHttpResponse}: Converts it to a {@link HttpResponse} with pre-populated content observable.</li>
 <li>Any other object: Assumes that it is a transformed HTTP content & pass it through to the content observable.</li>
 </ul>
 *
 * <h2>Writing Objects</h2>
 * <ul>
 <li>{@link HttpRequest}: Converts it to a {@link io.netty.handler.codec.http.HttpRequest}</li>
 <li>{@link ByteBuf} to an {@link HttpContent}</li>
 <li>Pass through any other message type.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public class ClientRequestResponseConverter extends ChannelDuplexHandler {

    @SuppressWarnings("rawtypes") private final PublishSubject contentSubject; // The type of this subject can change at runtime because a user can convert the content at runtime.
    @SuppressWarnings("rawtypes") private Observer requestProcessingObserver;

    public ClientRequestResponseConverter() {
        contentSubject = PublishSubject.create();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        if (io.netty.handler.codec.http.HttpResponse.class.isAssignableFrom(recievedMsgClass)) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            HttpResponse rxResponse = new HttpResponse((io.netty.handler.codec.http.HttpResponse)msg, contentSubject);
            super.channelRead(ctx, rxResponse); // For FullHttpResponse, this assumes that after this call returns,
                                                // someone has subscribed to the content observable, if not the content will be lost.
        }

        if (HttpContent.class.isAssignableFrom(recievedMsgClass)) {// This will be executed if the incoming message is a FullHttpResponse or only HttpContent.
            ByteBuf content = ((ByteBufHolder) msg).content();
            if (content.isReadable()) {
                invokeContentOnNext(content);
            }
            if (LastHttpContent.class.isAssignableFrom(recievedMsgClass)) {
                if (null != requestProcessingObserver) {
                    requestProcessingObserver.onCompleted();
                }
                contentSubject.onCompleted();
            }
        } else if(!io.netty.handler.codec.http.HttpResponse.class.isAssignableFrom(recievedMsgClass)){
            invokeContentOnNext(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        if (HttpRequest.class.isAssignableFrom(recievedMsgClass)) {
            HttpRequest<?> rxRequest = (HttpRequest<?>) msg;
            if (rxRequest.getHeaders().hasContent()) {
                MultipleFutureListener allWritesListener = new MultipleFutureListener(promise);
                allWritesListener.listen(ctx.write(rxRequest.getNettyRequest()));
                ContentSource<?> contentSource;
                if (rxRequest.hasRawContentSource()) {
                    contentSource = rxRequest.getRawContentSource();
                    @SuppressWarnings("rawtypes")
                    RawContentSource<?> rawContentSource = (RawContentSource) contentSource;
                    while (rawContentSource.hasNext()) {
                        @SuppressWarnings("rawtypes")
                        ContentTransformer transformer = rawContentSource.getTransformer();
                        @SuppressWarnings("unchecked")
                        ByteBuf byteBuf = transformer.transform(rawContentSource.next(), ctx.alloc());
                        allWritesListener.listen(ctx.write(byteBuf));
                    }
                } else {
                    contentSource = rxRequest.getContentSource();
                    while (contentSource.hasNext()) {
                        allWritesListener.listen(ctx.write(contentSource.next()));
                    }
                }
            } else {
                ctx.write(rxRequest.getNettyRequest(), promise);
            }
        } else {
            ctx.write(msg, promise); // pass through, since we do not understand this message.
        }
    }

    void setRequestProcessingObserver(@SuppressWarnings("rawtypes") Observer requestProcessingObserver) {
        this.requestProcessingObserver = requestProcessingObserver;
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
