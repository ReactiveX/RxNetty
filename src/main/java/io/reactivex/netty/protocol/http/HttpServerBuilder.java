package io.reactivex.netty.protocol.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.handler.codec.http.HttpObject;
import io.reactivex.netty.server.AbstractServerBuilder;

/**
 * A convenience builder to create instances of {@link HttpServer}
 *
 * @author Nitesh Kant
 */
public class HttpServerBuilder<I extends HttpObject, O>
        extends AbstractServerBuilder<I, O, HttpServerBuilder<I, O>, HttpServer<I, O>> {

    public HttpServerBuilder(int port) {
        super(port);
        pipelineConfigurator(new HttpServerPipelineConfigurator());
    }

    public HttpServerBuilder(ServerBootstrap bootstrap, int port) {
        super(bootstrap, port);
        pipelineConfigurator(new HttpServerPipelineConfigurator());
    }

    @Override
    protected HttpServer<I, O> createServer() {
        return new HttpServer<I, O>(serverBootstrap, port, pipelineConfigurator);
    }
}
