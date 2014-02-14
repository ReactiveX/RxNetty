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
package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.reactivex.netty.ConnectionHandler;
import io.reactivex.netty.ObservableConnection;
import rx.subjects.PublishSubject;

public class ConnectionLifecycleHandler<I, O> extends ChannelInboundHandlerAdapter {

    private final ConnectionHandler<I, O> connectionHandler;
    private final ObservableAdapter observableAdapter;
    private PublishSubject<I> inputSubject;
    private ObservableConnection<I,O> connection;

    public ConnectionLifecycleHandler(ConnectionHandler<I, O> connectionHandler, ObservableAdapter observableAdapter) {
        this.connectionHandler = connectionHandler;
        this.observableAdapter = observableAdapter;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (null != connection) {
            connection.close();
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        inputSubject = PublishSubject.create();
        connection = new ObservableConnection<I, O>(ctx, inputSubject);
        if (null != observableAdapter) {
            observableAdapter.activate(inputSubject);
        }
        super.channelActive(ctx);
        connectionHandler.handle(connection).subscribe();// TODO: Manage currently processing connections pool
    }
}
