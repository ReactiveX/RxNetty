package io.reactivex.netty.protocol.udp.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * An extension of {@link ObservableConnection} for UDP. The basic difference is that a UDP connection must always
 * eventually write a {@link DatagramPacket} by default.
 *
 * @author Nitesh Kant
 */
public class UdpClientConnection<I, O> extends ObservableConnection<I, O> {

    private final InetSocketAddress receiverAddress;

    public UdpClientConnection(ChannelHandlerContext ctx, InetSocketAddress receiverAddress) {
        super(ctx);
        this.receiverAddress = receiverAddress;
    }

    @Override
    public void writeBytes(byte[] msg) {
        ByteBuf data = getChannelHandlerContext().alloc().buffer(msg.length);
        data.writeBytes(msg);
        writeOnChannel(new DatagramPacket(data, receiverAddress));
    }

    @Override
    public Observable<Void> writeBytesAndFlush(byte[] msg) {
        writeBytes(msg);
        return flush();
    }

    @Override
    public void writeString(String msg) {
        byte[] dataBytes = msg.getBytes(Charset.defaultCharset());
        writeBytes(dataBytes);
    }

    @Override
    public Observable<Void> writeStringAndFlush(String msg) {
        writeString(msg);
        return flush();
    }
}
