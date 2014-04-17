package io.reactivex.netty.client;

import rx.Observer;

/**
 * A strategy to delegate the decision pertaining to {@link ConnectionPool} size limits.
 *
 * @author Nitesh Kant
 */
public interface PoolLimitDeterminationStrategy extends Observer<PoolInsightProvider.StateChangeEvent> {

    boolean acquireCreationPermit();

    int getAvailablePermits();
}
