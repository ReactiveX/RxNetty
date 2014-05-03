package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;

/**
 * An abstract factory for creating {@link ClientChannelFactory}
 *
 * @author Nitesh Kant
 */
public interface ClientChannelAbstractFactory<I, O> {

    ClientChannelFactory<I, O> newClientChannelFactory(RxClient.ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                       ObservableConnectionFactory<I, O> connectionFactory);
}
