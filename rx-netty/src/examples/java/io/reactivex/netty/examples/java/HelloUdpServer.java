package io.reactivex.netty.examples.java;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public final class HelloUdpServer {

    private static final byte[] WELCOME_MSG_BYTES = "Welcome to the broadcast world!".getBytes(Charset.defaultCharset());
    public static final int PORT = 8000;

    public static void main(String[] args) {
        RxNetty.createUdpServer(PORT, new ConnectionHandler<DatagramPacket, DatagramPacket>() {
            @Override
            public Observable<Void> handle(final ObservableConnection<DatagramPacket, DatagramPacket> newConnection) {
                System.out.println("HelloUdpServer.handle");
                return newConnection.getInput().flatMap(new Func1<DatagramPacket, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(DatagramPacket received) {
                        System.out.println("HelloUdpServer.call");
                        ByteBuf data = newConnection.getChannelHandlerContext().alloc().buffer(WELCOME_MSG_BYTES.length);
                        return newConnection.writeAndFlush(new DatagramPacket(data, received.sender()));
                    }
                });
            }
        }).startAndWait();
    }
}
