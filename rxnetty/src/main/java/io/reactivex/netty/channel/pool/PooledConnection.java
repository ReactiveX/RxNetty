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
package io.reactivex.netty.channel.pool;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge.ConnectionResueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;

import static io.reactivex.netty.protocol.http.client.HttpClientToConnectionBridge.*;

/**
 * An implementation of {@link Connection} which is pooled and reused.
 *
 * It is required to call {@link #reuse(Subscriber)} for reusing this connection.
 *
 * @param <R> Type of object that is read from this connection.
 * @param <W> Type of object that is written to this connection.
 */
public class PooledConnection<R, W> extends Connection<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(PooledConnection.class);

    private final Owner<R, W> owner;
    private final Connection<R, W> unpooledDelegate;

    private volatile long lastReturnToPoolTimeMillis;
    private volatile boolean releasedAtLeastOnce;
    private volatile long maxIdleTimeMillis;
    private final Observable<Void> releaseObservable;

    private PooledConnection(Owner<R, W> owner, PoolConfig<W, R> poolConfig, Connection<R, W> unpooledDelegate) {
        super(unpooledDelegate);
        if (null == owner) {
            throw new IllegalArgumentException("Pooled connection owner can not be null");
        }
        if (null == unpooledDelegate) {
            throw new IllegalArgumentException("Connection delegate can not be null");
        }
        if (null == unpooledDelegate) {
            throw new IllegalArgumentException("Pool config can not be null");
        }
        this.owner = owner;
        this.unpooledDelegate = unpooledDelegate;
        maxIdleTimeMillis = poolConfig.getMaxIdleTimeMillis();
        lastReturnToPoolTimeMillis = System.currentTimeMillis();
        releaseObservable = Observable.create(new OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                if (!isUsable()) {
                    PooledConnection.this.owner.discard(PooledConnection.this)
                                               .unsafeSubscribe(subscriber);
                } else {
                    Long keepAliveTimeout = unsafeNettyChannel().attr(KEEP_ALIVE_TIMEOUT_MILLIS_ATTR).get();
                    if (null != keepAliveTimeout) {
                        maxIdleTimeMillis = keepAliveTimeout;
                    }
                    markAwarePipeline.reset(); // Reset pipeline state, if changed, on release.
                    PooledConnection.this.owner.release(PooledConnection.this)
                         .doOnCompleted(new Action0() {
                             @Override
                             public void call() {
                                 releasedAtLeastOnce = true;
                                 lastReturnToPoolTimeMillis = System.currentTimeMillis();
                             }
                         })
                         .unsafeSubscribe(subscriber);
                }
            }
        }).onErrorResumeNext(discard());
    }

    @Override
    public Observable<Void> write(Observable<W> msgs) {
        return unpooledDelegate.write(msgs);
    }

    @Override
    public Observable<Void> write(Observable<W> msgs, Func1<W, Boolean> flushSelector) {
        return unpooledDelegate.write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<W> msgs) {
        return unpooledDelegate.writeAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs) {
        return unpooledDelegate.writeString(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs,
                                        Func1<String, Boolean> flushSelector) {
        return unpooledDelegate.writeString(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeStringAndFlushOnEach(Observable<String> msgs) {
        return unpooledDelegate.writeStringAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs) {
        return unpooledDelegate.writeBytes(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs,
                                       Func1<byte[], Boolean> flushSelector) {
        return unpooledDelegate.writeBytes(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return unpooledDelegate.writeBytesAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs) {
        return unpooledDelegate.writeFileRegion(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs,
                                            Func1<FileRegion, Boolean> flushSelector) {
        return unpooledDelegate.writeFileRegion(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeFileRegionAndFlushOnEach(Observable<FileRegion> msgs) {
        return unpooledDelegate.writeFileRegionAndFlushOnEach(msgs);
    }

    @Override
    public void flush() {
        unpooledDelegate.flush();
    }

    @Override
    public Observable<Void> close() {
        return close(true);
    }

    @Override
    public Observable<Void> close(boolean flush) {
        if (flush) {
            return releaseObservable.doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    unpooledDelegate.flush();
                }
            });
        } else {
            return releaseObservable;
        }
    }

    @Override
    public void closeNow() {
        close().subscribe(Actions.empty(), new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.error("Error closing connection.", throwable);
            }
        });
    }

    /**
     * Discards this connection, to be called when this connection will never be used again.
     *
     * @return {@link Observable} representing the result of the discard, this will typically be resulting in a close
     * on the underlying {@link Connection}.
     */
    public Observable<Void> discard() {
        return unpooledDelegate.close().finallyDo(new Action0() {
            @SuppressWarnings("unchecked")
            @Override
            public void call() {
                getEventsSubject().onEvent(ClientMetricsEvent.POOLED_CONNECTION_EVICTION);
            }
        });
    }

    /**
     * Returns whether this connection is safe to be used at this moment. <br/>
     * This makes sure that the underlying netty's channel is active as returned by
     * {@link Channel#isActive()} and it has not passed the maximum idle time in the pool.
     *
     * @return {@code true} if the connection is usable.
     */
    public boolean isUsable() {
        final Channel nettyChannel = unsafeNettyChannel();
        Boolean discardConn = nettyChannel.attr(DISCARD_CONNECTION).get();

        if (!nettyChannel.isActive() || Boolean.TRUE == discardConn) {
            return false;
        }

        long nowMillis = System.currentTimeMillis();
        long idleTime = nowMillis - lastReturnToPoolTimeMillis;
        return idleTime < maxIdleTimeMillis;
    }

    /**
     * This method must be called for reusing the connection i.e. for sending this connection to the passed subscriber.
     *
     * @param connectionSubscriber Subscriber for the pooled connection for reuse.
     */
    public void reuse(Subscriber<? super PooledConnection<R, W>> connectionSubscriber) {
        unsafeNettyChannel().pipeline().fireUserEventTriggered(new ConnectionResueEvent<R, W>(connectionSubscriber, this));
    }

    public static <R, W> PooledConnection<R, W> create(Owner<R, W> owner, PoolConfig<W, R> poolConfig,
                                                       Connection<R, W> unpooledDelegate) {
        final PooledConnection<R, W> toReturn = new PooledConnection<>(owner, poolConfig, unpooledDelegate);
        toReturn.connectCloseToChannelClose();
        return toReturn;
    }

    /**
     * Returns {@code true} if this connection is reused at least once.
     *
     * @return {@code true} if this connection is reused at least once.
     */
    public boolean isReused() {
        return releasedAtLeastOnce;
    }

    @Override
    public ChannelPipeline getChannelPipeline() {
        return markAwarePipeline; // Always return mark aware as, we always have to reset state on release to pool.
    }

    /*Visible for testin*/ void setLastReturnToPoolTimeMillis(long lastReturnToPoolTimeMillis) {
        this.lastReturnToPoolTimeMillis = lastReturnToPoolTimeMillis;
    }

    /**
     * A contract for the owner of the {@link PooledConnection} to which any instance of {@link PooledConnection} must
     * be returned after use.
     */
    public interface Owner<R, W> {

        /**
         * Releases the passed connection back to the owner, for reuse.
         *
         * @param connection Connection to be released.
         *
         * @return {@link Observable} representing result of the release. Every subscription to this, releases the
         * connection.
         */
        Observable<Void> release(PooledConnection<R, W> connection);

        /**
         * Discards the passed connection from the pool. This is usually called due to an external event like closing of
         * a connection that the pool may not know. <br/>
         * <b> This operation is idempotent and hence can be called multiple times with no side effects</b>
         *
         * @param connection The connection to discard.
         *
         * @return {@link Observable} indicating the result of the discard (which usually results in a close()).
         * Every subscription to this {@link Observable} will discard the connection.
         */
        Observable<Void> discard(PooledConnection<R, W> connection);

    }
}
