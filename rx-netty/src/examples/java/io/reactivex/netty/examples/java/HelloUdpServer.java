package io.reactivex.netty.examples.java;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.functions.Func1;

import java.net.InetSocketAddress;
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
                return newConnection.getInput().flatMap(new Func1<DatagramPacket, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(DatagramPacket received) {
                        InetSocketAddress sender = received.sender();
                        System.out.println("Received datagram. Sender: " + sender + ", data: "
                                           + received.content().toString(Charset.defaultCharset()));
                        ByteBuf data = newConnection.getChannelHandlerContext().alloc().buffer(WELCOME_MSG_BYTES.length);
                        data.writeBytes(WELCOME_MSG_BYTES);
                        return newConnection.writeAndFlush(new DatagramPacket(data, sender));
                    }
                });
            }
        }).startAndWait();
    }
}
