package io.reactivex.netty.channel;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Nitesh Kant
 */
public interface ObservableConnectionFactory<I, O> {

    ObservableConnection<I, O> newConnection(ChannelHandlerContext ctx);

}
