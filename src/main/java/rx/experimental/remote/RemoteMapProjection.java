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