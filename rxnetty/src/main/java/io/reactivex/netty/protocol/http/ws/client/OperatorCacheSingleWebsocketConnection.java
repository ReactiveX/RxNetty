package io.reactivex.netty.protocol.http.ws.client;

import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func1;

/**
 * An operator to cache a {@link WebSocketConnection} until it closes, upon which the source that re-creates an HTTP
 * upgrade request to get a fresh {@link WebSocketConnection} is subscribed, to refresh the stale connection in the
 * cache.
 *
 * A typical usage example for this operator is:
 *
 <pre>
 {@code
     HttpClient.newClient(socketAddress)
               .createGet("/ws")
               .requestWebSocketUpgrade()
               .map(WebSocketResponse::getWebSocketConnection)
               .nest()
               .lift(new OperatorCacheSingleWebsocketConnection())
 }
 </pre>
 *
 * Since multiple subscriptions to {@link WebSocketResponse#getWebSocketConnection()} do not re-run the original HTTP
 * upgrade request, this operator expects the source {@code Observable} to be passed to it, so that on close of the
 * cached {@link WebSocketConnection}, it can re-subscribe to the original HTTP request and create a fresh connection.
 * This is the reason the above code uses {@link Observable#nest()} to get a reference to the source {@code Observable}.
 *
 * <h2> Cache liveness guarantees</h2>
 *
 * Although, this operator will make sure that when the cached connection has terminated, the next refresh will
 * re-subscribe to the source, there is no guarantee that a dead connection is never emitted from this operator as it
 * completely depends on the timing of when the connection terminates and when a new subscription arrives. The two
 * events can be concurrent and hence unpredictable.
 */
@Experimental
public class OperatorCacheSingleWebsocketConnection
        implements Operator<WebSocketConnection, Observable<Observable<WebSocketConnection>>> {

    private boolean subscribedToSource; /*Guarded by this*/
    private Observable<WebSocketConnection> cachedSource; /*Guarded by this*/

    @Override
    public Subscriber<? super Observable<Observable<WebSocketConnection>>>
    call(final Subscriber<? super WebSocketConnection> subscriber) {

        return new Subscriber<Observable<Observable<WebSocketConnection>>>(subscriber) {

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
            public void onNext(Observable<Observable<WebSocketConnection>> source) {
                anItemEmitted = true;

                /**
                 * The idea below is for using a single cache {@code Observable} so that the cache operator can cache
                 * the generated connection. However, when the cached connection is terminated, a new cached source
                 * must be generated to be used for subsequent subscriptions.
                 * As the only way to re-run the original HTTP upgrade request, to obtain a fresh connection, is to
                 * subscribe to the {@code Observable<Observable<WebSocketConnection>>}, that is the reason the below
                 * code uses a {@code flatmap} to transform {@code Observable<Observable<WebSocketConnection>>} to an
                 * {@code Observable<WebSocketConnection>} and still keeping the ability to re-subscribe to the original
                 * {@code Observable<Observable<WebSocketConnection>>}.
                 */
                final Observable<WebSocketConnection> _cachedSource;
                final Observable<WebSocketConnection> o = source.flatMap(
                        new Func1<Observable<WebSocketConnection>, Observable<WebSocketConnection>>() {
                            @Override
                            public Observable<WebSocketConnection> call(Observable<WebSocketConnection> connSource) {
                                /*This is for flatmap to subscribe to the nested {@code Observable<WebSocketConnection>}*/
                                return connSource;
                            }
                        }).map(new Func1<WebSocketConnection, WebSocketConnection>() {
                    @Override
                    public WebSocketConnection call(WebSocketConnection connection) {
                        Observable<Void> lifecycle = connection.closeListener();
                        lifecycle = lifecycle.onErrorResumeNext(Observable.<Void>empty())
                                             .finallyDo(new Action0() {
                                                 @Override
                                                 public void call() {
                                                     synchronized (OperatorCacheSingleWebsocketConnection.this) {
                                                         // refresh the source on next subscribe
                                                         subscribedToSource = false;
                                                     }
                                                 }
                                             });
                        subscriber.add(lifecycle.subscribe(Actions.empty()));
                        return connection;
                    }
                }).cache();

                synchronized (OperatorCacheSingleWebsocketConnection.this) {
                    if (!subscribedToSource) {
                        subscribedToSource = true;
                        /*From here on, all subscriptions will use the newly created cached source which on first
                        subscription will re-run the original HTTP upgrade request and get a fresh WS connection*/
                        cachedSource = o;
                    }

                    _cachedSource = cachedSource;
                }

                _cachedSource.unsafeSubscribe(subscriber);
            }
        };
    }
}
