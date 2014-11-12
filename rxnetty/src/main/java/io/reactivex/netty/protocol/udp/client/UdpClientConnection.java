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
package io.reactivex.netty.protocol.udp.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.channel.ChannelMetricEventProvider;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsSubject;
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

    protected UdpClientConnection(Channel channel, InetSocketAddress receiverAddress,
                                  ChannelMetricEventProvider metricEventProvider,
                                  MetricEventsSubject<?> eventsSubject) {
        super(channel, metricEventProvider, eventsSubject);
        this.receiverAddress = receiverAddress;
    }

    public static <I, O> UdpClientConnection<I, O> create(final Channel channel,
                                                          InetSocketAddress receiverAddress,
                                                          final MetricEventsSubject<?> eventsSubject,
                                                          final ChannelMetricEventProvider metricEventProvider) {
        UdpClientConnection<I, O> toReturn = new UdpClientConnection<I, O>(channel, receiverAddress, metricEventProvider,
                                                                        eventsSubject);
        toReturn.fireNewRxConnectionEvent();
        return toReturn;
    }

    @Override
    public void writeBytes(byte[] msg) {
        ByteBuf data = getChannel().alloc().buffer(msg.length);
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
