package io.reactivex.netty.channel;

import io.netty.channel.ChannelHandlerContext;

/**
 * A factory to create {@link ObservableConnection}s
 *
 * @author Nitesh Kant
 */
public class UnpooledConnectionFactory<I, O> implements ObservableConnectionFactory<I, O> {

    @Override
    public ObservableConnection<I, O> newConnection(ChannelHandlerContext ctx) {
        return new ObservableConnection<I, O>(ctx);
    }
}
