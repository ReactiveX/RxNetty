package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class CompositePoolLimitDeterminationStrategy implements PoolLimitDeterminationStrategy {

    private final PoolLimitDeterminationStrategy[] strategies;

    public CompositePoolLimitDeterminationStrategy(PoolLimitDeterminationStrategy... strategies) {
        if (null == strategies || strategies.length == 0) {
            throw new IllegalArgumentException("Strategies can not be null or empty.");
        }
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            if (null == strategy) {
                throw new IllegalArgumentException("No strategy can be null.");
            }
        }
        this.strategies = strategies;
    }

    @Override
    public boolean acquireCreationPermit() {
        for (int i = 0; i < strategies.length; i++) {
            PoolLimitDeterminationStrategy strategy = strategies[i];
            if (!strategy.acquireCreationPermit()) {
                if (i > 0) {
                    for (int j = i - 1; j >= 0; j--) {
                        strategies[j].onNext(PoolInsightProvider.StateChangeEvent.ConnectFailed); // release all permits acquired before this failure.
                    }
                }
                return false;
            }
        }
        return true; // nothing failed and hence it is OK to create a new connection.
    }

    /**
     * Returns the minimum number of permits available across all strategies.
     *
     * @return The minimum number of permits available across all strategies.
     */
    @Override
    public int getAvailablePermits() {
        int minPermits = Integer.MAX_VALUE;
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            int availablePermits = strategy.getAvailablePermits();
            minPermits = Math.min(minPermits, availablePermits);
        }
        return minPermits; // If will atleast be one strategy (invariant in constructor) and hence this should be the value provided by that strategy.
    }

    @Override
    public void onCompleted() {
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            strategy.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            strategy.onError(e);
        }
    }

    @Override
    public void onNext(PoolInsightProvider.StateChangeEvent stateChangeEvent) {
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            strategy.onNext(stateChangeEvent);
        }
    }
}
