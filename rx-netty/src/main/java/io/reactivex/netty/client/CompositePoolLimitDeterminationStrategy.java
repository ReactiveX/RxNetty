package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class CompositePoolLimitDeterminationStrategy implements PoolLimitDeterminationStrategy {

    private final PoolLimitDeterminationStrategy[] strategies;

    public CompositePoolLimitDeterminationStrategy(PoolLimitDeterminationStrategy... strategies) {
        if (null == strategies) {
            throw new IllegalArgumentException("Strategies can not be null.");
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
            if (!strategy.acquireCreationPermit() && i > 0) {
                for (int j = i - 1; j >= 0; j--) {
                    strategies[j].onNext(PoolInsightProvider.StateChangeEvent.ConnectFailed); // release all permits acquired before this failure.
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
        int minPermits = 0;
        for (PoolLimitDeterminationStrategy strategy : strategies) {
            int availablePermits = strategy.getAvailablePermits();
            if (0 == minPermits) {
                minPermits = availablePermits;
            } else {
                minPermits = Math.min(minPermits, availablePermits);
            }
        }
        return minPermits;
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
