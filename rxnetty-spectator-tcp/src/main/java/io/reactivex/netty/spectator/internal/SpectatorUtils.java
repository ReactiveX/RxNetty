/*
 * Copyright 2015 Netflix, Inc.
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
 *
 */
package io.reactivex.netty.spectator.internal;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.histogram.PercentileTimer;

public final class SpectatorUtils {
    private SpectatorUtils() {
    }

    public static Counter newCounter(Registry registry, String name, String id) {
        return registry.counter(name, "id", id);
    }

    public static Counter newCounter(Registry registry, String name, String id, String... tags) {
        String[] allTags = getTagsWithId(id, tags);
        return registry.counter(name, allTags);
    }

    public static Timer newTimer(Registry registry, String name, String id) {
        return registry.timer(name, "id", id);
    }

    public static Timer newTimer(Registry registry, String name, String id, String... tags) {
        return registry.timer(name, getTagsWithId(id, tags));
    }

    public static <T extends Number> T newGauge(Registry registry, String name, String id, T number) {
        Id gaugeId = registry.createId(name, "id", id);
        return registry.gauge(gaugeId, number);
    }

    public static <T extends Number> T newGauge(Registry registry, String name, String id, T number, String... tags) {
        Id gaugeId = registry.createId(name, getTagsWithId(id, tags));
        return registry.gauge(gaugeId, number);
    }

    public static PercentileTimer newPercentileTimer(Registry registry, String name, String id, String... tags) {
        Id timerId = registry.createId(name, getTagsWithId(id, tags));
        return PercentileTimer.get(registry, timerId);
    }

    public static String[] mergeTags(String[] tags1, String... tags2) {
        if (tags1.length == 0) {
            return tags2;
        }
        if (tags2.length == 0) {
            return tags1;
        }

        String[] toReturn = new String[tags1.length + tags2.length];
        System.arraycopy(tags1, 0, toReturn, 0, tags1.length);
        System.arraycopy(tags2, 0, toReturn, tags1.length, tags2.length);
        return toReturn;
    }

    private static String[] getTagsWithId(String id, String[] tags) {
        String[] allTags = new String[tags.length + 2];
        System.arraycopy(tags, 0, allTags, 0, tags.length);
        allTags[allTags.length - 2] = "id";
        allTags[allTags.length - 1] = id;
        return allTags;
    }
}
