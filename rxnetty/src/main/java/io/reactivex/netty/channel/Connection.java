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
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.protocol.tcp.ConnectionInputSubscriberEvent;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

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
 */
public abstract class Connection<R, W> implements ChannelOperations<W> {

    private final Channel nettyChannel;

    protected final MarkAwarePipeline markAwarePipeline;
    private final ConnectionEventListener eventListener;
    private final EventPublisher eventPublisher;

    protected Connection(final Channel nettyChannel, ConnectionEventListener eventListener,
                         EventPublisher eventPublisher) {
        this.eventListener = eventListener;
        this.eventPublisher = eventPublisher;
        if (null == nettyChannel) {
            throw new IllegalArgumentException("Channel can not be null");
        }
        this.nettyChannel = nettyChannel;
        markAwarePipeline = new MarkAwarePipeline(nettyChannel.pipeline());
    }

    protected Connection(Connection<R, W> toCopy) {
        eventListener = toCopy.eventListener;
        eventPublisher = toCopy.eventPublisher;
        nettyChannel = toCopy.nettyChannel;
        markAwarePipeline = toCopy.markAwarePipeline;
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
     *
     * @return An {@link Observable}, subscription to which will discard the input. This {@code Observable} will
     * error/complete when the input errors/completes and unsubscription from here will unsubscribe from the content.
     */
    public Observable<Void> ignoreInput() {
        return getInput().map(new Func1<R, Void>() {
            @Override
            public Void call(R r) {
                ReferenceCountUtil.release(r);
                return null;
            }
        }).ignoreElements();
    }

    /**
     * Returns the {@link MarkAwarePipeline} for this connection, changes to which can be reverted at any point in time.
     */
    public MarkAwarePipeline getResettableChannelPipeline() {
        return markAwarePipeline;
    }

    /**
     * Returns the {@link ChannelPipeline} for this connection.
     *
     * @return {@link ChannelPipeline} for this connection.
     */
    public ChannelPipeline getChannelPipeline() {
        return nettyChannel.pipeline();
    }

    /**
     * Returns the underlying netty {@link Channel} for this connection.
     *
     * <h2>Why unsafe?</h2>
     *
     * It is advisable to use this connection abstraction for all interactions with the channel, however, advanced users
     * may find directly using the netty channel useful in some cases.
     *
     * @return The underlying netty {@link Channel} for this connection.
     */
    public Channel unsafeNettyChannel() {
        return nettyChannel;
    }

    /**
     * Returns an {@link Observable} that completes when this connection is closed.
     *
     * @return An {@link Observable} that completes when this connection is closed.
     */
    public Observable<Void> closeListener() {
        return Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                nettyChannel.closeFuture()
                            .addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture future) throws Exception {
                                    subscriber.onCompleted();
                                }
                            });
            }
        });
    }

    public ConnectionEventListener getEventListener() {
        return eventListener;
    }

    public EventPublisher getEventPublisher() {
        return eventPublisher;
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
