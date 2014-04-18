package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.ClientConnectionHandler;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;

/**
 * @author Nitesh Kant
 */
public class HttpClientChannelFactory<I, O> extends
        ClientChannelFactoryImpl<HttpClientResponse<O>, HttpClientRequest<I>> {

    public HttpClientChannelFactory(Bootstrap clientBootstrap,
                                    ObservableConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>> connectionFactory,
                                    RxClient.ServerInfo serverInfo) {
        super(clientBootstrap, connectionFactory, serverInfo);
    }

    @Override
    protected PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> getPipelineConfiguratorForAChannel(
            ClientConnectionHandler<HttpClientResponse<O>, HttpClientRequest<I>> connHandler,
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator) {
        PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> configurator =
                new PipelineConfiguratorComposite<HttpClientResponse<O>, HttpClientRequest<I>>(pipelineConfigurator, new ClientRequiredConfigurator<I, O>());
        return super.getPipelineConfiguratorForAChannel(connHandler, configurator);
    }
}
