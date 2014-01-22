package io.reactivex.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.http.HttpClient;
import io.reactivex.netty.http.HttpClientPipelineConfigurator;

/**
 * @author Nitesh Kant
 */
public class HttpClientBuilder<I extends HttpRequest, O>
        extends AbstractClientBuilder<I, O, HttpClientBuilder<I, O>, HttpClient<I, O>> {

    public HttpClientBuilder(String host, int port) {
        super(host, port);
        pipelineConfigurator(new HttpClientPipelineConfigurator());
    }

    public HttpClientBuilder(Bootstrap bootstrap, String host, int port) {
        super(bootstrap, host, port);
        pipelineConfigurator(new HttpClientPipelineConfigurator());
    }

    @Override
    protected HttpClient<I, O> createClient() {
        return new HttpClient<I, O>(serverInfo, bootstrap, pipelineConfigurator);
    }
}
