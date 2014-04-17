package io.reactivex.netty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link PoolLimitDeterminationStrategy} that limits the pool based on a maximum connections limit.
 * This limit can be increased or decreased at runtime.
 *
 * @author Nitesh Kant
 */
public class MaxConnectionsBasedStrategy implements PoolLimitDeterminationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MaxConnectionsBasedStrategy.class);

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

    public void onConnectFailed() {
        limitEnforcer.decrementAndGet();
    }

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

    @Override
    public void onCompleted() {
        // No op.
    }

    @Override
    public void onError(Throwable e) {
        logger.error("Connection pool emitted an error for state change events.", e);
    }

    @Override
    public void onNext(PoolInsightProvider.StateChangeEvent stateChangeEvent) {
        switch (stateChangeEvent) {
            case NewConnectionCreated:
                break;
            case ConnectFailed:
                onConnectFailed();
                break;
            case OnConnectionReuse:
                break;
            case OnConnectionEviction:
                onConnectionEviction();
                break;
            case onAcquireAttempted:
                break;
            case onAcquireSucceeded:
                break;
            case onAcquireFailed:
                break;
            case onReleaseAttempted:
                break;
            case onReleaseSucceeded:
                break;
            case onReleaseFailed:
                break;
        }
    }
}
