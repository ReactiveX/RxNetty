package io.reactivex.netty.protocol.http.ws.client;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.Subject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class OperatorCacheSingleWebsocketConnectionTest {

    @Rule
    public final OpRule opRule = new OpRule();

    @Test(timeout = 60000)
    public void testLifecycleNeverEnds() throws Exception {
        opRule.subscribeAndAssertValues(opRule.getSourceWithCache().repeat(2), 1);
    }

    @Test(timeout = 60000)
    public void testLifecycleCompletesImmediately() throws Exception {
        opRule.subscribeAndAssertValues(opRule.getSourceWithCache()
                                              .map(new Func1<WebSocketConnection, WebSocketConnection>() {
                                                  @Override
                                                  public WebSocketConnection call(WebSocketConnection c) {
                                                      opRule.terminateFirstConnection(null);
                                                      return c;
                                                  }
                                              }).repeat(2),
                                        2);// Since the cached item is immediately invalid, two items will be emitted from source.
    }

    @Test(timeout = 60000)
    public void testLifecycleErrorsImmediately() throws Exception {
        opRule.subscribeAndAssertValues(opRule.getSourceWithCache()
                                              .map(new Func1<WebSocketConnection, WebSocketConnection>() {
                                                  @Override
                                                  public WebSocketConnection call(WebSocketConnection c) {
                                                      opRule.terminateFirstConnection(new IllegalStateException());
                                                      return c;
                                                  }
                                              }).repeat(2),
                                        2);// Since the cached item is immediately invalid, two items will be emitted from source.
    }

    @Test(timeout = 60000)
    public void testSourceEmitsNoItems() throws Exception {
        TestSubscriber<WebSocketConnection> subscriber = new TestSubscriber<>();

        Observable.<Observable<Observable<WebSocketConnection>>>empty()
                  .lift(new OperatorCacheSingleWebsocketConnection())
                  .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(IllegalStateException.class);
    }

    @Test(timeout = 60000)
    public void testSourceEmitsError() throws Exception {
        TestSubscriber<WebSocketConnection> subscriber = new TestSubscriber<>();

        Observable.<Observable<WebSocketConnection>>error(new NullPointerException())
                  .nest()
                  .lift(new OperatorCacheSingleWebsocketConnection())
                  .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(NullPointerException.class);
    }

    @Test(timeout = 60000)
    public void testUnsubscribeFromLifecycle() throws Exception {

        opRule.subscribeAndAssertValues(opRule.getSourceWithCache(), 1);
        LifecycleSubject lifecycleSubject = opRule.lifecycles.poll();
        assertThat("No subscribers to lifecycle.", lifecycleSubject.subscribers, hasSize(1));
        assertThat("Lifecycle subscriber not unsubscribed.", lifecycleSubject.subscribers.poll().isUnsubscribed(),
                   is(true));
    }

    private static class OpRule extends ExternalResource {

        private Observable<Observable<Observable<WebSocketConnection>>> source;
        private ConcurrentLinkedQueue<LifecycleSubject> lifecycles;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    lifecycles = new ConcurrentLinkedQueue<>();
                    source = Observable.create(new OnSubscribe<Observable<WebSocketConnection>>() {
                        @Override
                        public void call(Subscriber<? super Observable<WebSocketConnection>> s) {
                            LifecycleSubject l = new LifecycleSubject(new ConcurrentLinkedQueue<Subscriber<? super Void>>());
                            lifecycles.add(l);
                            WebSocketConnection conn = newConnection(l);
                            /*Subscriptions to the emitted Observable will always give the same connection but
                            subscription to the source (this Observable) creates a new connection. This simulates
                            how the actual websocket connection get works.*/
                            s.onNext(Observable.just(conn));
                            s.onCompleted();

                        }
                    }).nest();
                    base.evaluate();
                }
            };
        }

        public boolean terminateFirstConnection(Throwable error) {
            LifecycleSubject poll = lifecycles.poll();

            if (null != poll) {
                if (null == error) {
                    poll.onCompleted();
                } else {
                    poll.onError(error);
                }
                return true;
            }

            return false;
        }

        public Observable<WebSocketConnection> getSourceWithCache() {
            return source.lift(new OperatorCacheSingleWebsocketConnection());
        }

        public void subscribeAndAssertValues(Observable<WebSocketConnection> source, int distinctItemsCount) {
            TestSubscriber<WebSocketConnection> subscriber = new TestSubscriber<>();

            source.subscribe(subscriber);

            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();

            List<WebSocketConnection> onNextEvents = subscriber.getOnNextEvents();
            Set<WebSocketConnection> distinctConns = new HashSet<>(onNextEvents);
            assertThat("Unexpected number of distinct connections.", distinctConns, hasSize(distinctItemsCount));
        }

        private WebSocketConnection newConnection(final Observable<Void> lifecycle) {
            @SuppressWarnings("unchecked")
            Connection<WebSocketFrame, WebSocketFrame> mock = Mockito.mock(Connection.class);
            Mockito.when(mock.closeListener()).thenAnswer(new Answer<Observable<Void>>() {
                @Override
                public Observable<Void> answer(InvocationOnMock invocation) throws Throwable {
                    return lifecycle;
                }
            });
            return new WebSocketConnection(mock);
        }
    }

    private static class LifecycleSubject extends Subject<Void, Void> {

        private final ConcurrentLinkedQueue<Subscriber<? super Void>> subscribers;

        protected LifecycleSubject(final ConcurrentLinkedQueue<Subscriber<? super Void>> subscribers) {
            super(new OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> subscriber) {
                    subscribers.add(subscriber);
                }
            });
            this.subscribers = subscribers;
        }

        @Override
        public boolean hasObservers() {
            return !subscribers.isEmpty();
        }

        @Override
        public void onCompleted() {
            for (Subscriber<? super Void> subscriber : subscribers) {
                subscriber.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            for (Subscriber<? super Void> subscriber : subscribers) {
                subscriber.onError(e);
            }
        }

        @Override
        public void onNext(Void aVoid) {
            // No op ...
        }
    }
}