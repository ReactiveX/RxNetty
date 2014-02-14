package io.reactivex.netty.protocol.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.AbstractServerBuilder;

import static io.reactivex.netty.protocol.http.server.HttpServer.HttpConnectionHandler;

/**
 * A convenience builder to create instances of {@link HttpServer}
 *
 * @author Nitesh Kant
 */
public class HttpServerBuilder<I, O> extends AbstractServerBuilder<HttpRequest<I>, HttpResponse<O>,
        HttpServerBuilder<I, O>, HttpServer<I, O>> {

    public HttpServerBuilder(int port, RequestHandler<I, O> requestHandler) {
        super(port, new HttpConnectionHandler<I, O>(requestHandler));
        pipelineConfigurator(PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    public HttpServerBuilder(ServerBootstrap bootstrap, int port, RequestHandler<I, O> requestHandler) {
        super(port, new HttpConnectionHandler<I, O>(requestHandler), bootstrap);
        pipelineConfigurator(PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    @Override
    protected HttpServer<I, O> createServer() {
        return new HttpServer<I, O>(serverBootstrap, port, pipelineConfigurator,
                                    (HttpConnectionHandler<I, O>) connectionHandler);
    }
}
