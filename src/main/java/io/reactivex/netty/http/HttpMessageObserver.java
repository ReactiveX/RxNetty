/**
 * Copyright 2013 Netflix, Inc.
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
 */
package io.reactivex.netty.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import rx.Observer;

/**
 * An inbound handler that observes incoming HTTP messages.
 */
public class HttpMessageObserver<T> extends ChannelInboundHandlerAdapter {

    private final Observer<? super ObservableHttpResponse<T>> observer;
    private volatile ObservableHttpResponse<T> response;

    public HttpMessageObserver(Observer<? super ObservableHttpResponse<T>> observer, ObservableHttpResponse<T> response) {
        if (observer == null) {
            throw new IllegalArgumentException("observer is null");
        }
        this.observer = observer;
        this.response = response;
    }

    // suppressing because Netty uses Object but we have typed HandlerObserver to I and expect only I
    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (response != null) {
            response.contentObserver().onNext((T) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (response != null) {
            response.contentObserver().onError(cause);
        } else {
            observer.onError(cause);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        response.contentObserver().onCompleted();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        if (evt == ObservableHttpResponse.Events.End) {
            response.contentObserver().onCompleted();    
            observer.onCompleted();
        }
    }
    
    
}
