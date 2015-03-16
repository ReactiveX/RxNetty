/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

/**
 * An abstraction over netty's channel providing Rx APIs.
 *
 * <h2>Reading data</h2>
 *
 * Unless, {@link ChannelOption#AUTO_READ} is set to {@code true} on the underneath channel, data will be read from the
 * connection if and only if there is a subscription to the input stream returned by {@link #getInput()}.
 * In case, the input data is not required to be consumed, one should call {@link #ignoreInput()}, otherwise, data will
 * never be read from the channel.
 *
 * @param <R> Type of object that is read from this connection.
 * @param <W> Type of object that is written to this connection.
 *
 * @author Nitesh Kant
 */
public abstract class Connection<R, W> implements ChannelOperations<W> {

    private final Channel nettyChannel;
    @SuppressWarnings("rawtypes")
    private final MetricEventsSubject eventsSubject;
    private final ChannelMetricEventProvider metricEventProvider;

    protected Connection(final Channel nettyChannel, MetricEventsSubject<?> eventsSubject,
                         ChannelMetricEventProvider metricEventProvider) {
        this.eventsSubject = eventsSubject;
        this.metricEventProvider = metricEventProvider;
        if (null == nettyChannel) {
            throw new IllegalArgumentException("Channel can not be null");
        }
        this.nettyChannel = nettyChannel;
    }

    protected Connection(Connection<R, W> toCopy) {
        eventsSubject = toCopy.eventsSubject;
        metricEventProvider = toCopy.metricEventProvider;
        nettyChannel = toCopy.nettyChannel;
    }

    /**
     * Returns a stream of data that is read from the connection.
     *
     * Unless, {@link ChannelOption#AUTO_READ} is set to {@code true}, the content will only be read from the
     * underneath channel, if there is a subscriber to the input.
     * In case, input is not required to be read, call {@link #ignoreInput()}
     *
     * @return The stream of data that is read from the connection.
     */
    public Observable<R> getInput() {
        return Observable.create(new OnSubscribe<R>() {
            @Override
            public void call(Subscriber<? super R> subscriber) {
                nettyChannel.pipeline()
                            .fireUserEventTriggered(new ConnectionInputSubscriberEvent<R, W>(subscriber,
                                                                                             Connection.this));
            }
        });
    }

    /**
     * Ignores all input on this connection.
     *
     * Unless, {@link ChannelOption#AUTO_READ} is set to {@code true}, the content will only be read from the
     * underneath channel, if there is a subscriber to the input. So, upon recieving this connection, either one should
     * call this method or eventually subscribe to the stream returned by {@link #getInput()}
     */
    public void ignoreInput() {
        nettyChannel.pipeline().fireUserEventTriggered(ConnectionInputSubscriberEvent.discardAllInput(this));
    }

    public Channel getNettyChannel() {
        return nettyChannel;
    }

    @SuppressWarnings("rawtypes")
    protected MetricEventsSubject getEventsSubject() {
        return eventsSubject;
    }

    protected ChannelMetricEventProvider getMetricEventProvider() {
        return metricEventProvider;
    }

    /*
     * In order to make sure that the connection is correctly initialized, the listener needs to be added post
     * constructor. Otherwise, there is a race-condition of the channel closed before the connection is completely
     * created and the Connection.close() call on channel close can access the Connection object which isn't
     * constructed completely. IOW, "this" escapes from the constructor if the listener is added in the constructor.
     */
    protected void connectCloseToChannelClose() {
        nettyChannel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                close(false); // Close this connection when the channel is closed.
            }
        });
    }
}
