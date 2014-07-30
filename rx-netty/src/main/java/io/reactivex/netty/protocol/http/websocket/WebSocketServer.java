package io.reactivex.netty.protocol.http.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.websocket.frame.WebSocketFrame;
import io.reactivex.netty.server.RxServer;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * {@link WebSocketServer} delays connection executing application handler
 * till WebSocket handshake is complete.

 * @author Tomasz Bak
 */
@SuppressWarnings("unchecked")
public class WebSocketServer<T extends WebSocketFrame> extends RxServer<T, T> {
    public WebSocketServer(ServerBootstrap bootstrap, int port, ConnectionHandler<T, T> connectionHandler) {
        super(bootstrap, port, new WrappedObservableConnectionHandler(connectionHandler));
    }

    public WebSocketServer(ServerBootstrap bootstrap, int port, PipelineConfigurator<T, T> pipelineConfigurator, ConnectionHandler<T, T> connectionHandler) {
        super(bootstrap, port, pipelineConfigurator, new WrappedObservableConnectionHandler(connectionHandler));
    }

    public WebSocketServer(ServerBootstrap bootstrap, int port, ConnectionHandler<T, T> connectionHandler, EventExecutorGroup connHandlingExecutor) {
        super(bootstrap, port, new WrappedObservableConnectionHandler(connectionHandler), connHandlingExecutor);
    }

    public WebSocketServer(ServerBootstrap bootstrap, int port, PipelineConfigurator<T, T> pipelineConfigurator, ConnectionHandler<T, T> connectionHandler, EventExecutorGroup connHandlingExecutor) {
        super(bootstrap, port, pipelineConfigurator, new WrappedObservableConnectionHandler(connectionHandler), connHandlingExecutor);
    }

    static class WrappedObservableConnectionHandler<T> implements ConnectionHandler<T, T> {

        private final ConnectionHandler<T, T> originalHandler;

        WrappedObservableConnectionHandler(ConnectionHandler<T, T> originalHandler) {
            this.originalHandler = originalHandler;
        }

        @Override
        public Observable<Void> handle(final ObservableConnection<T, T> connection) {
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
