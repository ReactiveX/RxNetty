package io.reactivex.netty.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Nitesh Kant
 */
public class CompositeStatsTest {

    @Test
    public void testCompositeStats() throws Exception {
        TestablePoolStats stats1 = new TestablePoolStats();
        stats1.idleCount.incrementAndGet();
        stats1.inUse.incrementAndGet();
        stats1.totalCount.incrementAndGet();
        stats1.pendingAcquire.incrementAndGet();
        stats1.pendingRelease.incrementAndGet();

        CompositePoolStats stats = new CompositePoolStats(stats1);

        Assert.assertEquals("Unexpected in use count", 1, stats.getInUseCount());
        Assert.assertEquals("Unexpected idle count", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected total count", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected pending acquire count", 1, stats.getPendingAcquireRequestCount());
        Assert.assertEquals("Unexpected pending release count", 1, stats.getPendingReleaseRequestCount());

        stats.addNewStats(stats1);

        Assert.assertEquals("Unexpected in use count after adding one more stats.", 2, stats.getInUseCount());
        Assert.assertEquals("Unexpected idle count after adding one more stats.", 2, stats.getIdleCount());
        Assert.assertEquals("Unexpected total count after adding one more stats.", 2, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected pending acquire count after adding one more stats.", 2, stats.getPendingAcquireRequestCount());
        Assert.assertEquals("Unexpected pending release count after adding one more stats.", 2,
                            stats.getPendingReleaseRequestCount());
    }

    private static class TestablePoolStats implements PoolStats {

        public final AtomicLong inUse = new AtomicLong();
        public final AtomicLong idleCount = new AtomicLong();
        public final AtomicLong totalCount = new AtomicLong();
        public final AtomicLong pendingAcquire = new AtomicLong();
        public final AtomicLong pendingRelease = new AtomicLong();

        @Override
        public long getInUseCount() {
            return inUse.get();
        }

        @Override
        public long getIdleCount() {
            return idleCount.get();
        }

        @Override
        public long getTotalConnectionCount() {
            return totalCount.get();
        }

        @Override
        public long getPendingAcquireRequestCount() {
            return pendingAcquire.get();
        }

        @Override
        public long getPendingReleaseRequestCount() {
            return pendingRelease.get();
        }
    }
}
