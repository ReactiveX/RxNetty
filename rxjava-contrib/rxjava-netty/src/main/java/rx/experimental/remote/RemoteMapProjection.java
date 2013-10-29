package rx.experimental.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoteMapProjection {

    private final List<String> values;

    RemoteMapProjection() {
        // empty for initial
        this.values = Collections.emptyList();
    }

    private RemoteMapProjection(RemoteMapProjection previous, String key) {
        List<String> newList = new ArrayList<String>(previous.values);
        newList.add(key);
        this.values = Collections.unmodifiableList(newList);
    }

    RemoteMapProjection with(String key) {
        return new RemoteMapProjection(this, key);
    }
}