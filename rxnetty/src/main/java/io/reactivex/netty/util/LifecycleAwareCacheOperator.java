package io.reactivex.netty.util;

import rx.Observable;
import rx.Observable.Operator;
import rx.Single;
import rx.Subscriber;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func1;

/**
 * An operator which is similar to {@link Observable#cache()} but is aware of the lifecycle of the cached item via an
 * {@code Observable<Void>}. When the cached item's lifecycle ends i.e. the associated {@code Observable} terminates,
 * the source {@link Observable} is re-subscribed and cached again.
 *
 * <h2> Cache liveness guarantees</h2>
 *
 * Although, this operator will make sure that when the cached item's lifecycle has terminated, the next refresh will
 * re-subscribe to the source, there is no guarantee that a dead item is never emitted from this operator as it
 * completely depends on the timing of when the lifecycle ends and when a new subscription arrives. The two events can
 * be concurrent and hence unpredictable.
 *
 * @param <T> The type of item emitted from this operator.
 */
@Experimental
public class LifecycleAwareCacheOperator<T> implements Operator<T, Single<T>> {

    private final Func1<T, Observable<Void>> lifecycleExtractor;
    private boolean subscribedToSource; /*Guarded by this*/
    private Observable<T> cachedSource; /*Guarded by this*/

    public LifecycleAwareCacheOperator(Func1<T, Observable<Void>> lifecycleExtractor) {
        this.lifecycleExtractor = lifecycleExtractor;
    }

    @Override
    public Subscriber<? super Single<T>> call(final Subscriber<? super T> subscriber) {

        return new Subscriber<Single<T>>(subscriber) {

            private volatile boolean anItemEmitted;

            @Override
            public void onCompleted() {
                if (!anItemEmitted) {
                    subscriber.onError(new IllegalStateException("No Observable emitted from source."));
                }
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(Single<T> single) {
                anItemEmitted = true;
                final Observable<T> _cachedSource;
                final Observable<T> o = single.map(new Func1<T, T>() {
                    @Override
                    public T call(T t) {
                        Observable<Void> lifecycle = lifecycleExtractor.call(t);
                        lifecycle = lifecycle.onErrorResumeNext(Observable.<Void>empty())
                                             .finallyDo(new Action0() {
                                                 @Override
                                                 public void call() {
                                                     synchronized (LifecycleAwareCacheOperator.this) {
                                                         // refresh the source on next subscribe
                                                         subscribedToSource = false;
                                                     }
                                                 }
                                             });
                        subscriber.add(lifecycle.subscribe(Actions.empty()));
                        return t;
                    }
                }).toObservable().cache();

                synchronized (LifecycleAwareCacheOperator.this) {
                    if (!subscribedToSource) {
                        subscribedToSource = true;
                        cachedSource = o;
                    }

                    _cachedSource = cachedSource;
                }

                _cachedSource.unsafeSubscribe(subscriber);
            }
        };
    }
}
