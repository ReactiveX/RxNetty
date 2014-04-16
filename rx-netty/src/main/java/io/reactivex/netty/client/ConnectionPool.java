package io.reactivex.netty.client;

import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;

/**
 * A pool of {@link PooledConnection}
 *
 * @author Nitesh Kant
 */
public interface ConnectionPool<I, O> extends ObservableConnectionFactory<I, O> {

    Observable<ObservableConnection<I, O>> acquire(PipelineConfigurator<I, O> pipelineConfigurator);

    Observable<Void> release(PooledConnection<I, O> connection);

    /**
     * Discards the passed connection from the pool. This is usually called due to an external event like closing of
     * a connection that the pool may not know. <br/>
     * <b> This operation is idempotent and hence can be called multiple times with no side effects</b>
     *
     * @param connection The connection to discard.
     *
     * @return Observable indicating the completion of the discard operation. Since, this operation is idempotent, the
     * returned observable does not indicate throw an error if the eviction of this connection was not due to that
     * particular invocation.
     */
    Observable<Void> discard(PooledConnection<I, O> connection);

    PoolStats getStats();

    void shutdown();
}
