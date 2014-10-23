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

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.metrics.MetricEventsSubject;

import java.net.InetSocketAddress;

/**
 * A factory to create {@link UdpClientConnection}
 *
 * @author Nitesh Kant
 */
class UdpClientConnectionFactory<I, O> implements ClientConnectionFactory<I, O, UdpClientConnection<I, O>> {

    private final InetSocketAddress receiverAddress;
    private MetricEventsSubject<?> eventsSubject;

    /**
     *
     * @param receiverAddress The default address for the {@link DatagramPacket} sent on the connections created by this
     *                        factory.
     */
    UdpClientConnectionFactory(InetSocketAddress receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    @Override
    public UdpClientConnection<I, O> newConnection(Channel channel) {
        return UdpClientConnection.create(channel, receiverAddress, eventsSubject,
                                          ClientChannelMetricEventProvider.INSTANCE);
    }

    @Override
    public void useMetricEventsSubject(MetricEventsSubject<?> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }
}
