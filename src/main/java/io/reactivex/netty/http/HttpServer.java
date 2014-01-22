package io.reactivex.netty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.handler.codec.http.HttpObject;
import io.reactivex.netty.NettyServer;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * @author Nitesh Kant
 */
public class HttpServer<I extends HttpObject, O> extends NettyServer<I, O> {

    public HttpServer(ServerBootstrap bootstrap, int port,
                      NettyPipelineConfigurator pipelineConfigurator) {
        super(bootstrap, port, pipelineConfigurator);
    }
}
