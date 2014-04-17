package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class CompositeStrategy implements PoolLimitDeterminationStrategy {

    private final PoolLimitDeterminationStrategy[] strategies;

    public CompositeStrategy(PoolLimitDeterminationStrategy... strategies) {
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
                    strategy.onNext(PoolInsightProvider.StateChangeEvent.ConnectFailed); // release all permits acquired before this failure.
                }
                break; // Break on first failure
            }
        }
        return true; // nothing failed and hence it is OK to create a new connection.
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
