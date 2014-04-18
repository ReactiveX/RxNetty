package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * A factory to create netty channels for clients.
 *
 * @param <I> The type of the object that is read from the channel created by this factory.
 * @param <O> The type of objects that are written to the channel created by this factory.
 *
 * @author Nitesh Kant
 */
public class ClientChannelFactoryImpl<I, O> implements ClientChannelFactory<I,O> {

    protected final Bootstrap clientBootstrap;
    private final ObservableConnectionFactory<I, O> connectionFactory;
    private final RxClient.ServerInfo serverInfo;

    public ClientChannelFactoryImpl(Bootstrap clientBootstrap, ObservableConnectionFactory<I, O> connectionFactory,
                                    RxClient.ServerInfo serverInfo) {
        this.clientBootstrap = clientBootstrap;
        this.connectionFactory = connectionFactory;
        this.serverInfo = serverInfo;
    }

    @Override
    public ChannelFuture connect(final Subscriber<? super ObservableConnection<I, O>> subscriber,
                                 PipelineConfigurator<I, O> pipelineConfigurator) {
        final ClientConnectionHandler<I, O> connHandler = new ClientConnectionHandler<I, O>(subscriber);

        final PipelineConfigurator<I, O> configurator = getPipelineConfiguratorForAChannel(connHandler,
                                                                                           pipelineConfigurator);
        // make the connection
        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                configurator.configureNewPipeline(ch.pipeline());
            }
        });

        final ChannelFuture connectFuture = clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort())
                                                           .addListener(connHandler);
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (!connectFuture.isDone()) {
                    connectFuture.cancel(true); // Unsubscribe here means, no more connection is required. A close on connection is explicit.
                }
            }
        }));

        return connectFuture;
    }


    protected PipelineConfigurator<I, O> getPipelineConfiguratorForAChannel(ClientConnectionHandler<I, O> connHandler,
                                                                            PipelineConfigurator<I, O> pipelineConfigurator) {
        RxRequiredConfigurator<I, O> requiredConfigurator = new RxRequiredConfigurator<I, O>(connHandler, connectionFactory);
        PipelineConfiguratorComposite<I, O> toReturn;
        if (null != pipelineConfigurator) {
            toReturn = new PipelineConfiguratorComposite<I, O>(pipelineConfigurator, requiredConfigurator);
        } else {
            toReturn = new PipelineConfiguratorComposite<I, O>(requiredConfigurator);
        }
        return toReturn;
    }
}
