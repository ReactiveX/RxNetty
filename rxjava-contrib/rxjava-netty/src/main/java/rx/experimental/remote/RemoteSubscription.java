package rx.experimental.remote;

import rx.Observable;

public abstract class RemoteSubscription {

    public abstract Observable<Void> unsubscribe();
}
