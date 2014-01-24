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
import io.reactivex.netty.ObservableConnection;
import rx.Observer;
import rx.subjects.PublishSubject;

public class ConnectionLifecycleHandler<I, O> extends ChannelInboundHandlerAdapter {

    private final Observer<? super ObservableConnection<I, O>> connectObserver;
    private final NettyObservableAdapter nettyObservableAdapter;
    private PublishSubject<I> responseSubject;

    public ConnectionLifecycleHandler(final Observer<? super ObservableConnection<I, O>> connectObserver,
                                      final NettyObservableAdapter nettyObservableAdapter) {
        this.connectObserver = connectObserver;
        this.nettyObservableAdapter = nettyObservableAdapter;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        connectObserver.onCompleted();
        if (null != responseSubject) {
            responseSubject.onCompleted();
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        responseSubject = PublishSubject.create();
        ObservableConnection<I, O> connection = new ObservableConnection<I, O>(ctx, responseSubject);
        if (null != nettyObservableAdapter) {
            nettyObservableAdapter.activate(responseSubject);
        }
        connectObserver.onNext(connection);
        super.channelActive(ctx);
    }
}