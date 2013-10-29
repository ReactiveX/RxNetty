package rx.experimental.remote;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Function;

public class RemoteObservableClient<T> {

    private final RemoteClientOnSubscribeFunc<T> onSubscribe;
    private final RemoteFilterCriteria f = new RemoteFilterCriteria();
    private final RemoteMapProjection m = new RemoteMapProjection();

    /**
     * Function interface for work to be performed when an {@link Observable} is subscribed to via {@link Observable#subscribe(Observer)}
     * 
     * @param <T>
     */
    public static interface RemoteClientOnSubscribeFunc<T> extends Function {

        public RemoteSubscription onSubscribe(Observer<? super T> observer, RemoteFilterCriteria filterCriteria, RemoteMapProjection mapProjection);

    }

    protected RemoteObservableClient(RemoteClientOnSubscribeFunc<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> RemoteObservableClient<T> create(RemoteClientOnSubscribeFunc<T> onSubscribe) {
        return new RemoteObservableClient<T>(onSubscribe);
    }

    public FilteredRemoteObservable<T> filter(Func1<RemoteFilterCriteria, RemoteFilterCriteria> criteria) {
        return new FilteredRemoteObservable<T>(onSubscribe, criteria.call(f), m);
    }

    public FilteredMappedRemoteObservable<T> map(Func1<RemoteMapProjection, RemoteMapProjection> projection) {
        return new FilteredMappedRemoteObservable<T>(onSubscribe, f, projection.call(m));
    }

    public <R> ConnectedRemoteObservableClient<T, R> onConnect(Func1<T, Observable<R>> func) {
        return new ConnectedRemoteObservableClient<T, R>(this, func);
    }

    public RemoteSubscription subscribe(Observer<T> observer) {
        return onSubscribe.onSubscribe(observer, f, m);
    }

    public static class ConnectedRemoteObservableClient<T, R> {

        private final RemoteObservableClient<T> remoteObservableClient;
        private final Func1<T, Observable<R>> func;

        private ConnectedRemoteObservableClient(RemoteObservableClient<T> remoteObservableClient, Func1<T, Observable<R>> func) {
            this.remoteObservableClient = remoteObservableClient;
            this.func = func;
        }

        public RemoteSubscription subscribe(final Observer<R> observer) {
            return remoteObservableClient.subscribe(new Observer<T>() {

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

        /**
         * An {@link Observer} must call an Observable's {@code subscribe} method in order to
         * receive items and notifications from the Observable.
         * 
         * @param onNext
         * @param onError
         * @param onComplete
         */
        public RemoteSubscription subscribe(final Action1<? super R> onNext) {
            if (onNext == null) {
                throw new IllegalArgumentException("onNext can not be null");
            }

            return subscribe(new Observer<R>() {

                @Override
                public void onCompleted() {
                    // do nothing
                }

                @Override
                public void onError(Throwable e) {
                    // throw since an onError handler was not provided
                    throw new RuntimeException("Error without onError handler", e);
                }

                @Override
                public void onNext(R args) {
                    onNext.call(args);
                }

            });
        }

        /**
         * An {@link Observer} must call an Observable's {@code subscribe} method in order to
         * receive items and notifications from the Observable.
         * 
         * @param onNext
         * @param onError
         * @param onComplete
         */
        public RemoteSubscription subscribe(final Action1<? super R> onNext, final Action1<Throwable> onError) {
            if (onNext == null) {
                throw new IllegalArgumentException("onNext can not be null");
            }
            if (onError == null) {
                throw new IllegalArgumentException("onError can not be null");
            }

            return subscribe(new Observer<R>() {

                @Override
                public void onCompleted() {
                    // do nothing
                }

                @Override
                public void onError(Throwable e) {
                    onError.call(e);
                }

                @Override
                public void onNext(R args) {
                    onNext.call(args);
                }

            });
        }

        /**
         * An {@link Observer} must call an Observable's {@code subscribe} method in order to
         * receive items and notifications from the Observable.
         * 
         * @param onNext
         * @param onError
         * @param onComplete
         */
        public RemoteSubscription subscribe(final Action1<? super R> onNext, final Action1<Throwable> onError, final Action0 onComplete) {
            if (onNext == null) {
                throw new IllegalArgumentException("onNext can not be null");
            }
            if (onError == null) {
                throw new IllegalArgumentException("onError can not be null");
            }
            if (onComplete == null) {
                throw new IllegalArgumentException("onComplete can not be null");
            }

            return subscribe(new Observer<R>() {

                @Override
                public void onCompleted() {
                    onComplete.call();
                }

                @Override
                public void onError(Throwable e) {
                    onError.call(e);
                }

                @Override
                public void onNext(R args) {
                    onNext.call(args);
                }

            });
        }

    }

    public static class FilteredRemoteObservable<T> {

        private final RemoteClientOnSubscribeFunc<T> onSubscribe;
        private final RemoteFilterCriteria f;
        private final RemoteMapProjection m;

        public FilteredRemoteObservable(RemoteClientOnSubscribeFunc<T> onSubscribe, RemoteFilterCriteria f, RemoteMapProjection m) {
            this.onSubscribe = onSubscribe;
            this.f = f;
            this.m = m;
        }

        public FilteredMappedRemoteObservable<T> map(Func1<RemoteMapProjection, RemoteMapProjection> projection) {
            return new FilteredMappedRemoteObservable<T>(onSubscribe, f, projection.call(m));
        }

        public RemoteSubscription subscribe(Observer<T> observer) {
            return onSubscribe.onSubscribe(observer, f, m);
        }
    }

    public static class FilteredMappedRemoteObservable<T> {

        private final RemoteClientOnSubscribeFunc<T> onSubscribe;
        private final RemoteFilterCriteria f;
        private final RemoteMapProjection m;

        public FilteredMappedRemoteObservable(RemoteClientOnSubscribeFunc<T> onSubscribe, RemoteFilterCriteria f, RemoteMapProjection m) {
            this.onSubscribe = onSubscribe;
            this.f = f;
            this.m = m;
        }

        public RemoteSubscription subscribe(Observer<T> observer) {
            return onSubscribe.onSubscribe(observer, f, m);
        }
    }

}
