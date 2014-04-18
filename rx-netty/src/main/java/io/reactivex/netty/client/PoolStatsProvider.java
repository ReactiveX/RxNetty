package io.reactivex.netty.client;

import rx.Observer;

/**
 * A provider for {@link PoolStats} for a pool which also listens for {@link PoolInsightProvider.PoolStateChangeEvent}
 * as that is the only way to update stats.
 *
 * @author Nitesh Kant
 */
public interface PoolStatsProvider extends Observer<PoolInsightProvider.PoolStateChangeEvent> {

    PoolStats getStats();
}
