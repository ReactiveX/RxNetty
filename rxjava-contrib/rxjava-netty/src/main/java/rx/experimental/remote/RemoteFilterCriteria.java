package rx.experimental.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RemoteFilterCriteria {

    private final Map<String, Object> values;

    RemoteFilterCriteria() {
        // empty for initial
        this.values = Collections.emptyMap();
    }

    private RemoteFilterCriteria(RemoteFilterCriteria previous, String key, Object value) {
        // create a new immutable map that is a concat of previous + new value
        HashMap<String, Object> newMap = new HashMap<String, Object>(previous.values);
        newMap.put(key, value);
        this.values = Collections.unmodifiableMap(newMap);
    }

    RemoteFilterCriteria with(String key, String value) {
        return new RemoteFilterCriteria(this, key, value);
    }

    RemoteFilterCriteria with(String key, Number value) {
        return new RemoteFilterCriteria(this, key, value);
    }
}