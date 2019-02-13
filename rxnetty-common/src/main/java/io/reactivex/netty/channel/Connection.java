/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Action1;
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

    public static final AttributeKey<Connection> CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("rx-netty-conn-attr");

    private final Channel nettyChannel;
    private final ContentSource<R> contentSource;
    protected final MarkAwarePipeline markAwarePipeline;

    protected Connection(final Channel nettyChannel) {
        if (null == nettyChannel) {
            throw new IllegalArgumentException("Channel can not be null");
        }
        this.nettyChannel = nettyChannel;
        markAwarePipeline = new MarkAwarePipeline(nettyChannel.pipeline());
        contentSource = new ContentSource<>(nettyChannel, new Func1<Subscriber<? super R>, Object>() {
            @Override
            public Object call(Subscriber<? super R> subscriber) {
                return new ConnectionInputSubscriberEvent<>(subscriber);
            }
        });
    }

    protected Connection(Connection<R, W> toCopy) {
        nettyChannel = toCopy.nettyChannel;
        markAwarePipeline = toCopy.markAwarePipeline;
        contentSource = toCopy.contentSource;
    }

    protected Connection(Connection<?, ?> toCopy, ContentSource<R> contentSource) {
        nettyChannel = toCopy.nettyChannel;
        markAwarePipeline = toCopy.markAwarePipeline;
        this.contentSource = contentSource;
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
    public ContentSource<R> getInput() {
        return contentSource;
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
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified handler is added at
     * the first position of the pipeline as specified by {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerFirst(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified handler is added at
     * the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param name     the name of the handler to append
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                       ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified handler is added at
     * the last position of the pipeline as specified by {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW>  addChannelHandlerLast(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified handler is added at
     * the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param name     the name of the handler to append
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                      ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerBefore(String baseName, String name,
                                                                        ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                        String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerAfter(String baseName, String name,
                                                                       ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for this connection. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler Handler instance to add.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                       String name, ChannelHandler handler);

    /**
     * Configures the {@link ChannelPipeline} for this channel, using the passed {@code pipelineConfigurator}.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return {@code this}.
     */
    public abstract <RR, WW> Connection<RR, WW> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Transforms this connection's input stream using the passed {@code transformer} to create a new
     * {@code Connection} instance.
     *
     * @param transformer Transformer to transform the input stream.
     *
     * @param <RR> New type of the input stream.
     *
     * @return A new connection instance with the transformed read stream.
     */
    public abstract <RR> Connection<RR, W> transformRead(Transformer<R, RR> transformer);

    /**
     * Transforms this connection to enable writing a different object type.
     *
     * @param transformer Transformer to transform objects written to the channel.
     *
     * @param <WW> New object types to be written to the connection.
     *
     * @return A new connection instance with the new write type.
     */
    public abstract <WW> Connection<R, WW> transformWrite(AllocatingTransformer<WW, W> transformer);

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

    /*
     * In order to make sure that the connection is correctly initialized, the listener needs to be added post
     * constructor. Otherwise, there is a race-condition of the channel closed before the connection is completely
     * created and the Connection.close() call on channel close can access the Connection object which isn't
     * constructed completely. IOW, "this" escapes from the constructor if the listener is added in the constructor.
     */
    protected void connectCloseToChannelClose() {
        nettyChannel.closeFuture()
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            closeNow(); // Close this connection when the channel is closed.
                        }
                    });
        nettyChannel.attr(CONNECTION_ATTRIBUTE_KEY).set(this);
    }
}
