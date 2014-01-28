package io.reactivex.netty.protocol.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.handler.codec.http.HttpObject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.RxServer;

/**
 * @author Nitesh Kant
 */
public class HttpServer<I extends HttpObject, O> extends RxServer<I, O> {

    public HttpServer(ServerBootstrap bootstrap, int port,
                      PipelineConfigurator<I, O> pipelineConfigurator) {
        super(bootstrap, port, pipelineConfigurator);
    }
}
