package io.reactivex.netty.server;

import io.netty.bootstrap.ServerBootstrap;

/**
 * A convenience builder for creating instances of {@link RxServer}
 *
 * @author Nitesh Kant
 */
public class ServerBuilder<I, O> extends AbstractServerBuilder<I,O, ServerBuilder<I, O>, RxServer<I, O>> {

    public ServerBuilder(int port) {
        super(port);
    }

    public ServerBuilder(int port, ServerBootstrap bootstrap) {
        super(bootstrap, port);
    }

    @Override
    protected RxServer<I, O> createServer() {
        return new RxServer<I, O>(serverBootstrap, port, pipelineConfigurator);
    }
}
