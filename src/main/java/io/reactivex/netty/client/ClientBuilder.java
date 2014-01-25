package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;

/**
 * A builder to build an instance of {@link RxClientImpl}
 *
 * @author Nitesh Kant
 */
public class ClientBuilder<I, O> extends AbstractClientBuilder<I,O, ClientBuilder<I, O>, RxClient<I, O>> {

    public ClientBuilder(String host, int port) {
        super(host, port);
    }

    public ClientBuilder(String host, int port, Bootstrap bootstrap) {
        super(bootstrap, host, port);
    }

    @Override
    protected RxClient<I, O> createClient() {
        return new RxClientImpl<I, O>(serverInfo, bootstrap, pipelineConfigurator);
    }
}
