/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
               }).toBlocking().forEach(new Action1<DatagramPacket>() {
            @Override
            public void call(DatagramPacket datagramPacket) {
                System.out.println("Received a new message: "
                                   + datagramPacket.content().toString(Charset.defaultCharset()));
            }
        });
    }
}
