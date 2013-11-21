/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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