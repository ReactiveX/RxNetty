package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.client.AbstractClientBuilder;
import io.reactivex.netty.pipeline.PipelineConfigurators;

/**
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
public class HttpClientBuilder<I, O>
        extends AbstractClientBuilder<HttpRequest<I>, HttpResponse<O>, HttpClientBuilder<I, O>, HttpClient<I, O>> {

    public HttpClientBuilder(String host, int port) {
        super(host, port);
        clientConfig = HttpClient.HttpClientConfig.DEFAULT_CONFIG;
        pipelineConfigurator(PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    public HttpClientBuilder(Bootstrap bootstrap, String host, int port) {
        super(bootstrap, host, port);
        pipelineConfigurator(PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    @Override
    protected HttpClient<I, O> createClient() {
        return new HttpClientImpl<I, O>(serverInfo, bootstrap, pipelineConfigurator, clientConfig);
    }
}
