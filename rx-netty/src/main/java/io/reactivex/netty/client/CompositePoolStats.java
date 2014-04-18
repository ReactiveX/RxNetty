package io.reactivex.netty.client;

import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A composite of {@link PoolStats} that provides a sum for all metrics of the contained {@link PoolStats}
 *
 * @author Nitesh Kant
 */
public class CompositePoolStats implements PoolStats {

    private final ConcurrentLinkedQueue<PoolStats> stats = new ConcurrentLinkedQueue<PoolStats>();

    public CompositePoolStats(PoolStats... stats) {
        if (null != stats) {
            Collections.addAll(this.stats, stats);
        }
    }

    /**
     * Adds a new stats instance to this composite.
     *
     * @param stats New stats to add.
     */
    public void addNewStats(PoolStats stats) {
        this.stats.add(stats);
    }

    @Override
    public long getInUseCount() {
        long toReturn = 0;
        for (PoolStats stat : stats) {
            toReturn += stat.getInUseCount();
        }
        return toReturn;
    }

    @Override
    public long getIdleCount() {
        long toReturn = 0;
        for (PoolStats stat : stats) {
            toReturn += stat.getIdleCount();
        }
        return toReturn;
    }

    @Override
    public long getTotalConnectionCount() {
        long toReturn = 0;
        for (PoolStats stat : stats) {
            toReturn += stat.getTotalConnectionCount();
        }
        return toReturn;
    }

    @Override
    public long getPendingAcquireRequestCount() {
        long toReturn = 0;
        for (PoolStats stat : stats) {
            toReturn += stat.getPendingAcquireRequestCount();
        }
        return toReturn;
    }

    @Override
    public long getPendingReleaseRequestCount() {
        long toReturn = 0;
        for (PoolStats stat : stats) {
            toReturn += stat.getPendingReleaseRequestCount();
        }
        return toReturn;
    }
}
