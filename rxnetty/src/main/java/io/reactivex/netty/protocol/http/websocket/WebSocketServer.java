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
package io.reactivex.netty.protocol.http.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.server.RxServer;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.List;

/**
 * {@link WebSocketServer} delays passing new connection to the application handler
 * till WebSocket handshake is complete.

 * @author Tomasz Bak
 */
public class WebSocketServer<I extends WebSocketFrame, O extends WebSocketFrame> extends RxServer<I, O> {
    @SuppressWarnings("unchecked")
    public WebSocketServer(ServerBootstrap bootstrap, int port, ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, null, new WrappedObservableConnectionHandler(connectionHandler));
    }

    @SuppressWarnings("unchecked")
    public WebSocketServer(ServerBootstrap bootstrap, int port, PipelineConfigurator<I, O> pipelineConfigurator, ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, pipelineConfigurator, new WrappedObservableConnectionHandler(connectionHandler), null);
    }

    @SuppressWarnings("unchecked")
    public WebSocketServer(ServerBootstrap bootstrap, int port, ConnectionHandler<I, O> connectionHandler, EventExecutorGroup connHandlingExecutor) {
        this(bootstrap, port, null, new WrappedObservableConnectionHandler(connectionHandler), connHandlingExecutor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public WebSocketServer(ServerBootstrap bootstrap, int port, PipelineConfigurator<I, O> pipelineConfigurator, ConnectionHandler<I, O> connectionHandler, EventExecutorGroup connHandlingExecutor) {
        super(bootstrap, port, pipelineConfigurator, new WrappedObservableConnectionHandler(connectionHandler), connHandlingExecutor);
        List<PipelineConfigurator> constituentConfigurators =
                ((PipelineConfiguratorComposite) this.pipelineConfigurator).getConstituentConfigurators();
        boolean updatedSubject = false;
        for (PipelineConfigurator configurator : constituentConfigurators) {
            if (configurator instanceof WebSocketServerPipelineConfigurator) {
                updatedSubject = true;
                WebSocketServerPipelineConfigurator<I, O> requiredConfigurator = (WebSocketServerPipelineConfigurator<I, O>) configurator;
                requiredConfigurator.useMetricEventsSubject(eventsSubject);
            }
        }
        if (!updatedSubject) {
            throw new IllegalStateException("No server required configurator added.");
        }
    }

    static class WrappedObservableConnectionHandler<I, O> implements ConnectionHandler<I, O> {

        private final ConnectionHandler<I, O> originalHandler;

        WrappedObservableConnectionHandler(ConnectionHandler<I, O> originalHandler) {
            this.originalHandler = originalHandler;
        }

        @Override
        public Observable<Void> handle(final ObservableConnection<I, O> connection) {
            final ChannelPipeline p = connection.getChannel().pipeline();
            ChannelHandlerContext hctx = p.context(WebSocketServerHandler.class);
            if (hctx != null) {
                WebSocketServerHandler handler = p.get(WebSocketServerHandler.class);
                final PublishSubject<Void> subject = PublishSubject.create();
                handler.addHandshakeFinishedListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        originalHandler.handle(connection).subscribe(subject);
                    }
                });
                return subject;
            }
            return originalHandler.handle(connection);
        }
    }
}
