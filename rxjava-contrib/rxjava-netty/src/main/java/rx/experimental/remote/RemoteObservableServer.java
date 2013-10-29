package rx.experimental.remote;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Function;

public class RemoteObservableServer<T> {

    private final RemoteServerOnSubscribeFunc<T> onSubscribe;

    /**
     * Function interface for work to be performed when an {@link Observable} is subscribed to via {@link Observable#subscribe(Observer)}
     * 
     * @param <T>
     */
    public static interface RemoteServerOnSubscribeFunc<T> extends Function {

        public Subscription onSubscribe(Observer<? super T> observer);

    }

    protected RemoteObservableServer(RemoteServerOnSubscribeFunc<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> RemoteObservableServer<T> create(RemoteServerOnSubscribeFunc<T> onSubscribe) {
        return new RemoteObservableServer<T>(onSubscribe);
    }

    public Subscription subscribe(Observer<? super T> observer) {
        return onSubscribe.onSubscribe(observer);
    }

    public <R> Observable<R> onConnect(Func1<T, Observable<R>> func) {
        return Observable.create(new ConnectedRemoteObservableServer<T, R>(this, func));
    }

    public static class ConnectedRemoteObservableServer<T, R> implements OnSubscribeFunc<R> {

        private final RemoteObservableServer<T> remoteObservableServer;
        private final Func1<T, Observable<R>> func;

        private ConnectedRemoteObservableServer(RemoteObservableServer<T> remoteObservableServer, Func1<T, Observable<R>> func) {
            this.remoteObservableServer = remoteObservableServer;
            this.func = func;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            return remoteObservableServer.subscribe(new Observer<T>() {

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(T t) {
                    try {
                        Observable<R> r = func.call(t);

                        // TODO may need SafeObserver wrapping this observer or assert onError/onCompleted/unsubscribe hasn't happened
                        r.subscribe(observer);
                    } catch (Throwable e) {
                        observer.onError(e);
                    }
                }
            });
        }

    }
}
