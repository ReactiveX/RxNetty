package io.reactivex.netty.client;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nitesh Kant
 */
public class PoolLimitStrategyTest {

    @Test
    public void testMaxConnectionLimit() throws Exception {

        MaxConnectionsBasedStrategy strategy = new MaxConnectionsBasedStrategy(3);
        Assert.assertTrue("Invalid permit acquire failure.", strategy.acquireCreationPermit());
        Assert.assertEquals("Unexpected available permits.", 2, strategy.getAvailablePermits());
        Assert.assertTrue("Invalid permit acquire failure.", strategy.acquireCreationPermit());
        Assert.assertEquals("Unexpected available permits.", 1, strategy.getAvailablePermits());
        Assert.assertTrue("Invalid permit acquire failure.", strategy.acquireCreationPermit());
        Assert.assertEquals("Unexpected available permits.", 0, strategy.getAvailablePermits());

        Assert.assertFalse("Invalid permit acquire success.", strategy.acquireCreationPermit());

        strategy.onNext(PoolInsightProvider.StateChangeEvent.OnConnectionEviction);

        Assert.assertEquals("Unexpected available permits.", 1, strategy.getAvailablePermits());
        Assert.assertTrue("Permit not available after release.", strategy.acquireCreationPermit());
    }

    @Test
    public void testCompositeStrategy() throws Exception {
        MaxConnectionsBasedStrategy global = new MaxConnectionsBasedStrategy(1);
        MaxConnectionsBasedStrategy local = new MaxConnectionsBasedStrategy(2);
        CompositePoolLimitDeterminationStrategy strategy =
                new CompositePoolLimitDeterminationStrategy(local, global);

        Assert.assertTrue("Invalid permit acquire failure.", strategy.acquireCreationPermit());
        Assert.assertEquals("Unexpected available global permits.", 0, global.getAvailablePermits());
        Assert.assertEquals("Unexpected available local permits.", 1, local.getAvailablePermits());
        Assert.assertEquals("Unexpected available composite permits.", 0, strategy.getAvailablePermits()); // Should be min. of all strategies

        Assert.assertFalse("Invalid permit acquire success.", strategy.acquireCreationPermit());

        Assert.assertEquals("Unexpected available global permits.", 0, global.getAvailablePermits());
        Assert.assertEquals("Unexpected available local permits.", 1, local.getAvailablePermits());
        Assert.assertEquals("Unexpected available composite permits.", 0, strategy.getAvailablePermits()); // Should be min. of all strategies

        strategy.onNext(PoolInsightProvider.StateChangeEvent.OnConnectionEviction);

        Assert.assertEquals("Unexpected available global permits.", 1, global.getAvailablePermits());
        Assert.assertEquals("Unexpected available local permits.", 2, local.getAvailablePermits());
        Assert.assertEquals("Unexpected available composite permits.", 1, strategy.getAvailablePermits()); // Should be min. of all strategies

        Assert.assertTrue("Invalid permit acquire failure.", strategy.acquireCreationPermit());
        Assert.assertEquals("Unexpected available global permits.", 0, global.getAvailablePermits());
        Assert.assertEquals("Unexpected available local permits.", 1, local.getAvailablePermits());
        Assert.assertEquals("Unexpected available composite permits.", 0, strategy.getAvailablePermits()); // Should be min. of all strategies
    }
}
