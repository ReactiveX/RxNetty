package io.reactivex.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.reactivex.netty.ConnectionHandler;

/**
 * A convenience builder for creating instances of {@link RxServer}
 *
 * @author Nitesh Kant
 */
public class ServerBuilder<I, O> extends AbstractServerBuilder<I,O, ServerBuilder<I, O>, RxServer<I, O>> {

    public ServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        super(port, connectionHandler);
    }

    public ServerBuilder(int port, ConnectionHandler<I, O> connectionHandler, ServerBootstrap bootstrap) {
        super(port, connectionHandler, bootstrap);
    }

    @Override
    protected RxServer<I, O> createServer() {
        return new RxServer<I, O>(serverBootstrap, port, pipelineConfigurator, connectionHandler);
    }
}
