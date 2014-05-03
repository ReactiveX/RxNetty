/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.contexts;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of {@link ContextKeySupplier} which is backed by a {@link Map}
 *
 * @author Nitesh Kant
 */
public class MapBackedKeySupplier implements ContextKeySupplier {

    private final Map<String, String> keys;

    public MapBackedKeySupplier() {
        this(new HashMap<String, String>());
    }

    public MapBackedKeySupplier(final Map<String, String> keys) {
        this.keys = keys;
    }

    @Override
    public String getContextValue(String key) {
        return keys.get(key);
    }

    public void put(String key, String value) {
        keys.put(key, value);
    }
}
