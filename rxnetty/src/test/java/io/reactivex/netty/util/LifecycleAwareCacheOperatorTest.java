package io.reactivex.netty.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.Single;
import rx.Single.OnSubscribe;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class LifecycleAwareCacheOperatorTest {

    @Rule
    public final OpRule opRule = new OpRule();

    @Test(timeout = 60000)
    public void testLifecycleNeverEnds() throws Exception {
        opRule.subscribeAndAssertValues(opRule.getSourceWithCache(new Func1<String, Observable<Void>>() {
            @Override
            public Observable<Void> call(String s) {
                return Observable.never();
            }
        }).repeat(2), "Hello 1", "Hello 1");
    }

    @Test(timeout = 60000)
    public void testLifecycleCompletesImmediately() throws Exception {
        opRule.subscribeAndAssertValues(opRule.getSourceWithCache(new Func1<String, Observable<Void>>() {
            @Override
            public Observable<Void> call(String s) {
                return Observable.empty();
            }
        }).repeat(2), "Hello 1", "Hello 2");// Since the cached item is immediately invalid,
                                            // two items will be emitted from source.
    }

    @Test(timeout = 60000)
    public void testLifecycleErrorsImmediately() throws Exception {
        opRule.subscribeAndAssertValues(opRule.getSourceWithCache(new Func1<String, Observable<Void>>() {
            @Override
            public Observable<Void> call(String s) {
                return Observable.error(new IllegalStateException());
            }
        }).repeat(2), "Hello 1", "Hello 2");// Since the cached item is immediately invalid,
                                            // two items will be emitted from source.
    }

    @Test(timeout = 60000)
    public void testSourceEmitsNoItems() throws Exception {
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        Observable.<Single<String>>empty()
                  .lift(new LifecycleAwareCacheOperator<>(new Func1<String, Observable<Void>>() {
                      @Override
                      public Observable<Void> call(String s) {
                          return Observable.never();
                      }
                  }))
                  .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(IllegalStateException.class);
    }

    @Test(timeout = 60000)
    public void testSourceEmitsError() throws Exception {
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        Observable.<Single<String>>error(new NullPointerException())
                  .lift(new LifecycleAwareCacheOperator<>(new Func1<String, Observable<Void>>() {
                      @Override
                      public Observable<Void> call(String s) {
                          return Observable.never();
                      }
                  }))
                  .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(NullPointerException.class);
    }

    @Test(timeout = 60000)
    public void testUnsubscribeFromLifecycle() throws Exception {

        final ConcurrentLinkedQueue<Subscriber<? super Void>> lifecycleSubs = new ConcurrentLinkedQueue<>();

        opRule.subscribeAndAssertValues(opRule.getSourceWithCache(new Func1<String, Observable<Void>>() {
            @Override
            public Observable<Void> call(String s) {
                return Observable.create(new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        lifecycleSubs.add(subscriber);
                    }
                });
            }
        }), "Hello 1");// Since the cached item is immediately invalid, two items will be emitted from source.

        assertThat("No subscribers to lifecycle.", lifecycleSubs, hasSize(1));
        assertThat("Lifecycle subscriber not unsubscribed.", lifecycleSubs.poll().isUnsubscribed(), is(true));
    }

    private static class OpRule extends ExternalResource {

        private Observable<Single<String>> source;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    source = Observable.just(Single.create(new OnSubscribe<String>() {

                        private final AtomicInteger subCount = new AtomicInteger();

                        @Override
                        public void call(SingleSubscriber<? super String> s) {
                            s.onSuccess("Hello " + subCount.incrementAndGet());
                        }
                    }));
                    base.evaluate();
                }
            };
        }

        public Observable<String> getSourceWithCache(Func1<String, Observable<Void>> lifeExtractor) {
            return source.lift(new LifecycleAwareCacheOperator<>(lifeExtractor));
        }

        public void subscribeAndAssertValues(Observable<String> source, String... values) {
            TestSubscriber<String> subscriber = new TestSubscriber<>();

            source.subscribe(subscriber);

            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();
            subscriber.assertValues(values);
        }
    }

}