package io.reactivex.netty.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.http.client.ClientRequestResponseConverter;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extension of {@link ObservableConnection} that is used by {@link ConnectionPool}
 *
 * @param <I> The type of the object that is read from this connection.
 * @param <O> The type of objects that are written to this connection.
 */
public class PooledConnection<I, O> extends ObservableConnection<I, O> {

    private final AtomicBoolean acquiredOrSoonToBeDiscarded = new AtomicBoolean(); // Being paranoid on the name as this
                                                                                   // is exactly what it is doing and I don't want this flag use to be overloaded.

    private final ConnectionPool<I, O> pool;

    private volatile long lastReturnToPoolTimeMillis;
    private volatile long maxIdleTimeMillis;

    public PooledConnection(ChannelHandlerContext ctx, ConnectionPool<I, O> pool) {
        this(ctx, pool, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());
    }

    public PooledConnection(ChannelHandlerContext ctx, ConnectionPool<I, O> pool, long maxIdleTimeMillis) {
        super(ctx);
        this.pool = pool;
        lastReturnToPoolTimeMillis = System.currentTimeMillis();
        this.maxIdleTimeMillis = maxIdleTimeMillis;
    }

    @Override
    public Observable<Void> close() {
        acquiredOrSoonToBeDiscarded.compareAndSet(true, false); // There isn't anything else to be done here.

        if (!isUsable()) {
            pool.discard(this); // This is the case where multiple close are invoked on the same connection.
            // One results in release and then the other result in discard if the call was
            // because of an underlying channel close.
        }

        return super.close();
    }

    @Override
    protected Observable<Void> _closeChannel() {
        Long keepAliveTimeout = getChannelHandlerContext().channel()
                                .attr(ClientRequestResponseConverter.KEEP_ALIVE_TIMEOUT_MILLIS_ATTR).get();
        if (null != keepAliveTimeout) {
            maxIdleTimeMillis = keepAliveTimeout;
        }

        final Observable<Void> release = pool.release(this);
        /**
         * Other way of doing this is release.finallyDo() but that would depend on whether someone subscribes to the
         * returned observable or not, which is not guaranteed, specially since its a close() call.
         */
        lastReturnToPoolTimeMillis = System.currentTimeMillis();
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
        Boolean discardConn = getChannelHandlerContext().channel()
                              .attr(ClientRequestResponseConverter.DISCARD_CONNECTION).get();

        if (!getChannelHandlerContext().channel().isActive() || Boolean.TRUE == discardConn) {
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
        ConnectionReuseEvent reuseEvent = new ConnectionReuseEvent(newInputSubject);
        getChannelHandlerContext().fireUserEventTriggered(reuseEvent);
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
}
