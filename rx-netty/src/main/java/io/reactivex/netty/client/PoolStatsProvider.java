package io.reactivex.netty.client;

import rx.Observer;

import static io.reactivex.netty.client.PoolInsightProvider.StateChangeEvent;

/**
 * A provider for {@link PoolStats} for a pool which also listens for {@link StateChangeEvent} as that is the only way
 * to update stats.
 *
 * @author Nitesh Kant
 */
public interface PoolStatsProvider extends Observer<StateChangeEvent> {

    PoolStats getStats();
}
