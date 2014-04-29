package io.reactivex.netty.examples.java;

import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.nio.charset.Charset;

/**
 * @author Nitesh Kant
 */
public final class HelloUdpClient {

    public static void main(String[] args) {
        RxNetty.createUdpClient("localhost", HelloUdpServer.PORT).connect()
               .flatMap(new Func1<ObservableConnection<DatagramPacket, DatagramPacket>,
                       Observable<DatagramPacket>>() {
                   @Override
                   public Observable<DatagramPacket> call(ObservableConnection<DatagramPacket, DatagramPacket> connection) {
                       connection.writeStringAndFlush("Is there anybody out there?");
                       return connection.getInput();
                   }
               }).toBlockingObservable().forEach(new Action1<DatagramPacket>() {
            @Override
            public void call(DatagramPacket datagramPacket) {
                System.out.println("Received a new message: "
                                   + datagramPacket.content().toString(Charset.defaultCharset()));
            }
        });
    }
}
