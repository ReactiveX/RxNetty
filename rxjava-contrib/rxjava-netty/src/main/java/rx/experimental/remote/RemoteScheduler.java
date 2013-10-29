package rx.experimental.remote;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

public abstract class RemoteScheduler {

    public <T> RemoteSubscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return null;
    }

    /**
     * Parallelism available to a Scheduler.
     * <p>
     * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
     * 
     * @return the scheduler's available degree of parallelism.
     */
    public int degreeOfParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

}
