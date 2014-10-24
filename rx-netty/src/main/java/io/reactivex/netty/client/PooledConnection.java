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

package io.reactivex.netty.client;

import io.netty.channel.Channel;
import io.reactivex.netty.channel.ChannelMetricEventProvider;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.client.ClientRequestResponseConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extension of {@link ObservableConnection} that is used by {@link ConnectionPool}.
 * <b>The pool using this connection must call {@link #setConnectionPool(ConnectionPool)} before using the instance.</b>
 * Failure to do so will never return this connection to the pool and the {@link #close()} will dispose this connection.
 *
 * @param <I> The type of the object that is read from this connection.
 * @param <O> The type of objects that are written to this connection.
 */
public class PooledConnection<I, O> extends ObservableConnection<I, O> {

    private static final Logger logger = LoggerFactory.getLogger(PooledConnection.class);

    private final AtomicBoolean acquiredOrSoonToBeDiscarded = new AtomicBoolean(); // Being paranoid on the name as this
                                                                                   // is exactly what it is doing and I don't want this flag use to be overloaded.

    private ConnectionPool<I, O> pool;

    private volatile long lastReturnToPoolTimeMillis;
    private volatile long maxIdleTimeMillis;

    protected PooledConnection(Channel channel, long maxIdleTimeMillis,
                               ChannelMetricEventProvider metricEventProvider, MetricEventsSubject<?> eventsSubject) {
        super(channel, metricEventProvider, eventsSubject);
        lastReturnToPoolTimeMillis = System.currentTimeMillis();
        this.maxIdleTimeMillis = maxIdleTimeMillis;
    }

    public void setConnectionPool(ConnectionPool<I, O> pool) {
        this.pool = pool;
    }

    @Override
    public Observable<Void> close() {
        acquiredOrSoonToBeDiscarded.compareAndSet(true, false); // There isn't anything else to be done here.

        if (!isUsable()) {
            discard(); // This is the case where multiple close are invoked on the same connection.
            // One results in release and then the other result in discard if the call was
            // because of an underlying channel close.
        }

        return super.close();
    }

    @Override
    protected Observable<Void> _closeChannel() {
        Long keepAliveTimeout = getChannel().attr(ClientRequestResponseConverter.KEEP_ALIVE_TIMEOUT_MILLIS_ATTR).get();
        if (null != keepAliveTimeout) {
            maxIdleTimeMillis = keepAliveTimeout;
        }

        final Observable<Void> release;
        if (null != pool) {

            cancelPendingWrites(true); // Cancel pending writes before releasing to the pool.

            release = pool.release(this);
            /**
             * Other way of doing this is release.finallyDo() but that would depend on whether someone subscribes to the
             * returned observable or not, which is not guaranteed, specially since its a close() call.
             */
            lastReturnToPoolTimeMillis = System.currentTimeMillis();
        } else {
            logger.warn("Connection pool instance not set in the PooledConnection. Discarding this connection.");
            release = super._closeChannel();
        }

        return release;
    }

    public Observable<Void> closeUnderlyingChannel() {
        return super._closeChannel();
    }

    /**
     * Returns whether this connection is safe to be used at this moment. <br/>
     * This makes sure that the underlying netty's channel is active as returned by
     * {@link Channel#isActive()} and it has not passed the maximum idle time in the pool.
     *
     * @return {@code true} if the connection is usable.
     */
    public boolean isUsable() {
        Boolean discardConn = getChannel().attr(ClientRequestResponseConverter.DISCARD_CONNECTION).get();

        if (!getChannel().isActive() || Boolean.TRUE == discardConn) {
            return false;
        }

        long nowMillis = System.currentTimeMillis();
        long idleTime = nowMillis - lastReturnToPoolTimeMillis;
        return idleTime < maxIdleTimeMillis;
    }

    public void beforeReuse() {
        closeIssued.set(false); // So that close can be called after reuse.
        PublishSubject<I> newInputSubject = PublishSubject.create();
        updateInputSubject(newInputSubject);
        ConnectionReuseEvent reuseEvent = new ConnectionReuseEvent(this, newInputSubject);
        getChannel().pipeline().fireUserEventTriggered(reuseEvent);
    }

    public void updateMaxIdleTimeMillis(long maxIdleTimeMillis) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
    }

    public static <I, O> PooledConnection<I, O> create(Channel channel, long maxIdleTimeMillis,
                                                       ChannelMetricEventProvider metricEventProvider,
                                                       MetricEventsSubject<?> eventsSubject) {
        final PooledConnection<I, O> toReturn = new PooledConnection<I, O>(channel, maxIdleTimeMillis, metricEventProvider,
                                                                           eventsSubject);
        toReturn.fireNewRxConnectionEvent();
        return toReturn;
    }

    public static <I, O> PooledConnection<I, O> create(Channel channel,
                                                       ChannelMetricEventProvider metricEventProvider,
                                                       MetricEventsSubject<?> eventsSubject) {
        return create(channel, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis(), metricEventProvider, eventsSubject);
    }

    /*Visible for testing*/ void setLastReturnToPoolTimeMillis(long lastReturnToPoolTimeMillis) {
        this.lastReturnToPoolTimeMillis = lastReturnToPoolTimeMillis;
    }

    /**
     * Claims the connection, till {@link #close()} is called.
     *
     * @return {@code true} if the connection could be claimed, else {@code false}
     */
    /*Package private to be used only by ConnectionPoolImp. The contract is too weak to be public*/ boolean claim() {
        return acquiredOrSoonToBeDiscarded.compareAndSet(false, true);
    }

    private void discard() {
        if (null == pool) {
            logger.warn("Connection pool instance not set in the PooledConnection.");
        } else {
            pool.discard(this);
        }
    }
}
