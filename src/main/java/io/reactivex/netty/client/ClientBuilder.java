package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;

/**
 * A builder to build an instance of {@link NettyClient}
 *
 * @author Nitesh Kant
 */
public class ClientBuilder<I, O> extends AbstractClientBuilder<I,O, ClientBuilder<I, O>, NettyClient<I, O>> {

    public ClientBuilder(String host, int port) {
        super(host, port);
    }

    public ClientBuilder(String host, int port, Bootstrap bootstrap) {
        super(bootstrap, host, port);
    }

    @Override
    protected NettyClient<I, O> createClient() {
        return new NettyClient<I, O>(serverInfo, bootstrap, pipelineConfigurator);
    }
}
