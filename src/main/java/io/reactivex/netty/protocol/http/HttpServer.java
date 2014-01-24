package io.reactivex.netty.protocol.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.handler.codec.http.HttpObject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.NettyServer;

/**
 * @author Nitesh Kant
 */
public class HttpServer<I extends HttpObject, O> extends NettyServer<I, O> {

    public HttpServer(ServerBootstrap bootstrap, int port,
                      PipelineConfigurator pipelineConfigurator) {
        super(bootstrap, port, pipelineConfigurator);
    }
}
