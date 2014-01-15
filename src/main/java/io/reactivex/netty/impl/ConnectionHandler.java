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
package io.reactivex.netty.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import rx.Observer;
import rx.util.functions.Action1;

public class ConnectionHandler<I, O> extends ChannelInboundHandlerAdapter {

    private final Action1<? super ObservableConnection<I, O>> onConnect;
    private volatile ObservableConnection<I, O> connection;

    public ConnectionHandler(Action1<? super ObservableConnection<I, O>> onConnect) {
        this.onConnect = onConnect;
    }
    
    public ConnectionHandler(final Observer<? super ObservableConnection<I, O>> observer) {
        this.onConnect = new Action1<ObservableConnection<I, O>>() {

            @Override
            public void call(ObservableConnection<I, O> connection) {
                observer.onNext(connection);
            }
            
        };
    }

    // suppressing because Netty uses Object but we have typed HandlerObserver to I and expect only I
    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        connection.getInputObserver().onNext((I) msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connection != null) {
            connection.getInputObserver().onError(cause);
        } else {
            /**
             * An exception occurred before a connection was activated in 'channelActive'
             * so we need to create an ObservableConnection, pass it down and then emit the error
             */
            connection = ObservableConnection.create(ctx);
            onConnect.call(connection);
            connection.getInputObserver().onError(new RuntimeException("Error occurred and connection does not exist: " + cause));
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        connection.getInputObserver().onCompleted();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        connection = ObservableConnection.create(ctx);
        onConnect.call(connection);
    }

}