package io.reactivex.netty.client;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link PoolLimitDeterminationStrategy} that limits the pool based on a maximum connections limit.
 * This limit can be increased or decreased at runtime.
 *
 * @author Nitesh Kant
 */
public class MaxConnectionsBasedStrategy extends AbstractLimitDeterminationStrategy {

    public static final int DEFAULT_MAX_CONNECTIONS = 1000;

    private final AtomicInteger limitEnforcer;
    private final AtomicInteger maxConnections;

    public MaxConnectionsBasedStrategy() {
        this(DEFAULT_MAX_CONNECTIONS);
    }

    public MaxConnectionsBasedStrategy(int maxConnections) {
        this.maxConnections = new AtomicInteger(maxConnections);
        limitEnforcer = new AtomicInteger();
    }

    @Override
    public boolean acquireCreationPermit() {
        /**
         * As opposed to limitEnforcer.incrementAndGet() we follow this model as this does not change the limitEnforcer
         * value unless there are enough permits.
         * If we were to use incrementAndGet(), in case of overflow (from max allowed limit) we would have to decrement
         * the limitEnforcer. This may show temporary overflows in getMaxConnections() which may be disturbing for a
         * user. However, even if we use incrementAndGet() the counter corrects itself over time.
         * This is just a more semantically correct implementation with similar performance characterstics as
         * incrementAndGet()
         */
        for (;;) {
            final int currentValue = limitEnforcer.get();
            final int newValue = currentValue + 1;
            final int maxAllowedConnections = maxConnections.get();
            if (newValue <= maxAllowedConnections) {
                if (limitEnforcer.compareAndSet(currentValue, newValue)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void onConnectFailed() {
        limitEnforcer.decrementAndGet();
    }

    @Override
    public void onConnectionEviction() {
        limitEnforcer.decrementAndGet();
    }

    public int incrementMaxConnections(int incrementBy) {
        return maxConnections.addAndGet(incrementBy);
    }

    public int decrementMaxConnections(int decrementBy) {
        return maxConnections.addAndGet(-1 * decrementBy);
    }

    public int getMaxConnections() {
        return maxConnections.get();
    }
}
