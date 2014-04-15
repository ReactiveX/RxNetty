package io.reactivex.netty.client;

/**
 * A strategy to delegate the decision pertaining to {@link ConnectionPool} size limits.
 *
 * @author Nitesh Kant
 */
public interface PoolLimitDeterminationStrategy extends PoolStateChangeListener{

    boolean acquireCreationPermit();
}
