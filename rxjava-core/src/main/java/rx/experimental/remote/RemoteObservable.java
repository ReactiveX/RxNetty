package rx.experimental.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Function;

public class RemoteObservable<T> {

    private final RemoteOnSubscribeFunc<T> onSubscribe;
    private final FilterCriteria f = new FilterCriteria();
    private final MapProjection m = new MapProjection();

    /**
     * Function interface for work to be performed when an {@link Observable} is subscribed to via {@link Observable#subscribe(Observer)}
     * 
     * @param <T>
     */
    public static interface RemoteOnSubscribeFunc<T> extends Function {

        public RemoteSubscription onSubscribe(RemoteObserver<? super T> observer, FilterCriteria filterCriteria, MapProjection mapProjection);

    }

    protected RemoteObservable(RemoteOnSubscribeFunc<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> RemoteObservable<T> create(RemoteOnSubscribeFunc<T> onSubscribe) {
        return new RemoteObservable<T>(onSubscribe);
    }

    public FilteredRemoteObservable<T> filter(Func1<FilterCriteria, FilterCriteria> criteria) {
        return new FilteredRemoteObservable<T>(onSubscribe, criteria.call(f), m);
    }

    public FilteredMappedRemoteObservable<T> map(Func1<MapProjection, MapProjection> projection) {
        return new FilteredMappedRemoteObservable<T>(onSubscribe, f, projection.call(m));
    }

    public RemoteSubscription subscribe(RemoteObserver<T> observer) {
        return onSubscribe.onSubscribe(observer, f, m);
    }

    public static class FilteredRemoteObservable<T> {

        private final RemoteOnSubscribeFunc<T> onSubscribe;
        private final FilterCriteria f;
        private final MapProjection m;

        public FilteredRemoteObservable(RemoteOnSubscribeFunc<T> onSubscribe, FilterCriteria f, MapProjection m) {
            this.onSubscribe = onSubscribe;
            this.f = f;
            this.m = m;
        }

        public FilteredMappedRemoteObservable<T> map(Func1<MapProjection, MapProjection> projection) {
            return new FilteredMappedRemoteObservable<T>(onSubscribe, f, projection.call(m));
        }

        public RemoteSubscription subscribe(RemoteObserver<T> observer) {
            return onSubscribe.onSubscribe(observer, f, m);
        }
    }

    public static class FilteredMappedRemoteObservable<T> {

        private final RemoteOnSubscribeFunc<T> onSubscribe;
        private final FilterCriteria f;
        private final MapProjection m;

        public FilteredMappedRemoteObservable(RemoteOnSubscribeFunc<T> onSubscribe, FilterCriteria f, MapProjection m) {
            this.onSubscribe = onSubscribe;
            this.f = f;
            this.m = m;
        }

        public RemoteSubscription subscribe(RemoteObserver<T> observer) {
            return onSubscribe.onSubscribe(observer, f, m);
        }
    }

    public static class FilterCriteria {

        private final Map<String, Object> values;

        private FilterCriteria() {
            // empty for initial
            this.values = Collections.emptyMap();
        }

        private FilterCriteria(FilterCriteria previous, String key, Object value) {
            // create a new immutable map that is a concat of previous + new value
            HashMap<String, Object> newMap = new HashMap<String, Object>(previous.values);
            newMap.put(key, value);
            this.values = Collections.unmodifiableMap(newMap);
        }

        FilterCriteria with(String key, String value) {
            return new FilterCriteria(this, key, value);
        }

        FilterCriteria with(String key, Number value) {
            return new FilterCriteria(this, key, value);
        }
    }

    public static class MapProjection {

        private final List<String> values;

        private MapProjection() {
            // empty for initial
            this.values = Collections.emptyList();
        }

        private MapProjection(MapProjection previous, String key) {
            List<String> newList = new ArrayList<String>(previous.values);
            newList.add(key);
            this.values = Collections.unmodifiableList(newList);
        }

        MapProjection with(String key) {
            return new MapProjection(this, key);
        }
    }

}
