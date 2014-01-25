package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.protocol.http.HttpClient;
import io.reactivex.netty.protocol.http.HttpClientImpl;
import io.reactivex.netty.protocol.http.HttpClientPipelineConfigurator;

/**
 * @author Nitesh Kant
 */
public class HttpClientBuilder<I extends HttpRequest, O>
        extends AbstractClientBuilder<I, O, HttpClientBuilder<I, O>, HttpClient<I, O>> {

    private HttpClient.RequestConfig globalRequestConfig;

    public HttpClientBuilder(String host, int port) {
        super(host, port);
        pipelineConfigurator(new HttpClientPipelineConfigurator<I, O>());
    }

    public HttpClientBuilder(Bootstrap bootstrap, String host, int port) {
        super(bootstrap, host, port);
        pipelineConfigurator(new HttpClientPipelineConfigurator<I, O>());
    }

    public HttpClientBuilder<I, O> globalRequestConfig(HttpClient.RequestConfig globalRequestConfig) {
        this.globalRequestConfig = globalRequestConfig;
        return returnBuilder();
    }

    @Override
    protected HttpClient<I, O> createClient() {
        return new HttpClientImpl<I, O>(serverInfo, bootstrap, pipelineConfigurator, globalRequestConfig);
    }
}
