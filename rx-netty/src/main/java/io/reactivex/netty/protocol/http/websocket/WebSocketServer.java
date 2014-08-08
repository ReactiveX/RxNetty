package io.reactivex.netty.protocol.http.websocket;

import java.util.List;

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
            ChannelHandlerContext ctx = connection.getChannelHandlerContext();
            final ChannelPipeline p = ctx.channel().pipeline();
            ChannelHandlerContext hctx = p.context(WebSocketServerHandler.class);
            if (hctx != null) {
                WebSocketServerHandler handler = p.get(WebSocketServerHandler.class);
                final PublishSubject<Void> subject = PublishSubject.create();
                handler.handshakeFuture().addListener(new ChannelFutureListener() {
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
