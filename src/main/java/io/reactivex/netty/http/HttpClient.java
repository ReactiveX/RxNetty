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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.netty.NettyClient;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.http.sse.codec.SSEEvent;
import io.reactivex.netty.spi.NettyPipelineConfigurator;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class HttpClient<I extends HttpRequest, O> extends NettyClient<I, O> {

    public HttpClient(ServerInfo serverInfo, Bootstrap clientBootstrap,
                      NettyPipelineConfigurator pipelineConfigurator) {
        super(serverInfo, clientBootstrap, pipelineConfigurator);
    }

    public Observable<ObservableHttpResponse<O>> observe(I request) {
        Observable<ObservableConnection<O, I>> connectionObservable = connect();

        connectionObservable.flatMap(new Func1<ObservableConnection<O, I>, Observable<ObservableHttpResponse<O>>>() {
            @Override
            public Observable<ObservableHttpResponse<O>> call(ObservableConnection<O, I> observableConnection) {
                Observable<ObservableHttpResponse<O>> responseObservable =
                        observableConnection.getInput().map(new Func1<O, ObservableHttpResponse<O>>() {
                            @Override
                            public ObservableHttpResponse<O> call(O o) {

                                return null;
                            }
                        });

                return responseObservable;
            }
        });
        return null;
    }
}