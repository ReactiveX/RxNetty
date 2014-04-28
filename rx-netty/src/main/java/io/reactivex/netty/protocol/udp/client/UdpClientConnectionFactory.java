package io.reactivex.netty.protocol.udp.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.ObservableConnectionFactory;

import java.net.InetSocketAddress;

/**
 * A factory to create {@link UdpClientConnection}
 *
 * @author Nitesh Kant
 */
class UdpClientConnectionFactory<I, O> implements ObservableConnectionFactory<I, O> {

    private final InetSocketAddress receiverAddress;

    /**
     *
     * @param receiverAddress The default address for the {@link DatagramPacket} sent on the connections created by this
     *                        factory.
     */
    UdpClientConnectionFactory(InetSocketAddress receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    @Override
    public ObservableConnection<I, O> newConnection(ChannelHandlerContext ctx) {
        return new UdpClientConnection<I, O>(ctx, receiverAddress);
    }
}
