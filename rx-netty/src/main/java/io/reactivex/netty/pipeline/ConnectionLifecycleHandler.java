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
package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.server.ErrorHandler;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

public class ConnectionLifecycleHandler<I, O> extends ChannelInboundHandlerAdapter {

    private final ConnectionHandler<I, O> connectionHandler;
    private final ObservableAdapter observableAdapter;
    private final ErrorHandler errorHandler;
    private PublishSubject<I> inputSubject;
    private ObservableConnection<I,O> connection;

    public ConnectionLifecycleHandler(ConnectionHandler<I, O> connectionHandler, ObservableAdapter observableAdapter,
                                      ErrorHandler errorHandler) {
        this.connectionHandler = connectionHandler;
        this.observableAdapter = observableAdapter;
        this.errorHandler = null == errorHandler ? new DefaultErrorHandler() : errorHandler;
    }

    public ConnectionLifecycleHandler(ConnectionHandler<I, O> connectionHandler, ObservableAdapter observableAdapter) {
        this(connectionHandler, observableAdapter, null);
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
        try {
            Observable<Void> handledObservable = connectionHandler.handle(connection);
            if (null == handledObservable) {
                handledObservable = Observable.empty();
            }

            System.err.println("handledObservable.subscribe");
            
            handledObservable.subscribe(new Subscriber<Void>() {
                @Override
                public void onCompleted() {
                    System.err.println("onCompleted()");
                    Thread.dumpStack();
                    connection.close();
                }

                @Override
                public void onError(Throwable e) {
                    System.err.println("onError(e)");
                    invokeErrorHandler(e);
                    connection.close();
                }

                @Override
                public void onNext(Void aVoid) {
                    System.err.println("onNext(void)");
                    // No Op.
                }
            });
        } catch (Throwable throwable) {
            invokeErrorHandler(throwable);
        }
    }

    private void invokeErrorHandler(Throwable throwable) {
        try {
            errorHandler.handleError(throwable);
        } catch (Exception e) {
            System.err.println("Error while invoking error handler. Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
