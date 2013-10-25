package rx.experimental.remote;

import rx.Observable;

public interface RemoteObserver<T> {

    public Observable<Void> onNext(T t);

    public Observable<Void> onError(Throwable t);

    public Observable<Void> onCompleted();
}
