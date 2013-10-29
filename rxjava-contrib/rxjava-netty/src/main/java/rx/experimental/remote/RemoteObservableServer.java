package rx.experimental.remote;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Function;

public class RemoteObservableServer<T> {

    private final RemoteServerOnSubscribeFunc<T> onSubscribe;

    /**
     * Function interface for work to be performed when an {@link Observable} is subscribed to via {@link Observable#subscribe(Observer)}
     * 
     * @param <T>
     */
    public static interface RemoteServerOnSubscribeFunc<T> extends Function {

        public Subscription onSubscribe(RemoteObserver<? super T> observer);

    }

    protected RemoteObservableServer(RemoteServerOnSubscribeFunc<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> RemoteObservableServer<T> create(RemoteServerOnSubscribeFunc<T> onSubscribe) {
        return new RemoteObservableServer<T>(onSubscribe);
    }

    public Subscription subscribe(RemoteObserver<? super T> observer) {
        return onSubscribe.onSubscribe(observer);
    }

}
