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

package io.reactivex.netty.examples.udp;

import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;

import static io.reactivex.netty.examples.udp.HelloUdpServer.DEFAULT_PORT;

/**
 * @author Nitesh Kant
 */
public final class HelloUdpClient {

    private final int port;

    public HelloUdpClient(int port) {
        this.port = port;
    }

    public String sendHello() {
        String content = RxNetty.<DatagramPacket, DatagramPacket>newUdpClientBuilder("localhost", port)
                                .enableWireLogging(LogLevel.ERROR).build().connect()
                .flatMap(new Func1<ObservableConnection<DatagramPacket, DatagramPacket>,
                        Observable<DatagramPacket>>() {
                    @Override
                    public Observable<DatagramPacket> call(ObservableConnection<DatagramPacket, DatagramPacket> connection) {
                        connection.writeStringAndFlush("Is there anybody out there?");
                        return connection.getInput();
                    }
                })
                .take(1)
                .map(new Func1<DatagramPacket, String>() {
                    @Override
                    public String call(DatagramPacket datagramPacket) {
                        return datagramPacket.content().toString(Charset.defaultCharset());
                    }
                })
                .toBlocking()
                .first();
        System.out.println("Received: " + content);
        return content;
    }

    public static void main(String[] args) {
        new HelloUdpClient(DEFAULT_PORT).sendHello();
    }

}
