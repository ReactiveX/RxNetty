package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class CompositeStrategy extends CompositePoolStateChangeListener implements PoolLimitDeterminationStrategy {

    private final PoolLimitDeterminationStrategy[] strategies;

    public CompositeStrategy(PoolLimitDeterminationStrategy... strategies) {
        this.strategies = strategies;
    }

    @Override
    public boolean acquireCreationPermit() {
        for (int i = 0; i < strategies.length; i++) {
            PoolLimitDeterminationStrategy strategy = strategies[i];
            if (!strategy.acquireCreationPermit() && i > 0) {
                for (int j = i - 1; j >= 0; j--) {
                    strategy.onConnectFailed(); // release all permits acquired before this failure.
                }
                break; // Break on first failure
            }
        }
        return true; // nothing failed and hence it is OK to create a new connection.
    }
}
