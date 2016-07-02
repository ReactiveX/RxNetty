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

import com.netflix.numerus.NumerusProperty;
import com.netflix.numerus.NumerusRollingPercentile;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;

import java.util.concurrent.TimeUnit;

import static com.netflix.numerus.NumerusProperty.Factory.*;

/**
 * A latency metric for publishing various percentiles of latency which spectator does not.
 */
public class LatencyMetrics {

    public enum Percentile {
        P5(5.0, "5"),
        P25(25.0, "25"),
        P50(50.0, "50"),
        P75(75.0, "75"),
        P90(90.0, "90"),
        P99(99.0, "99"),
        P99_5(99.5, "99.5"),
        MAX(100.0, "Max"),
        Min(0.0, "Min"),
        Mean(50.0, "Mean"),
        ;

        private final double v;
        private final String tag;

        Percentile(double v, String tag) {
            this.v = v;
            this.tag = tag;
        }

        public double getValue() {
            return v;
        }

        public String getTag() {
            return tag;
        }
    }

    private static final NumerusProperty<Integer> latency_timeInMilliseconds = asProperty(60000);
    private static final NumerusProperty<Integer> latency_numberOfBuckets = asProperty(12); // 12 buckets at 5000ms each
    private static final NumerusProperty<Integer> latency_bucketDataLength = asProperty(1000);
    private static final NumerusProperty<Boolean> latency_enabled = asProperty(true);

    private final NumerusRollingPercentile p;

    public LatencyMetrics(String metricName, String monitorId) {
        this(metricName, monitorId, Spectator.globalRegistry());
    }

    public LatencyMetrics(String metricName, String monitorId, Registry registry) {
        p = new NumerusRollingPercentile(latency_timeInMilliseconds, latency_numberOfBuckets, latency_bucketDataLength,
                                         latency_enabled);

        for (Percentile percentile : Percentile.values()) {
            registerPercentileGauge(metricName, monitorId, registry, percentile.v, percentile.getTag());
        }
    }

    public void record(long duration, TimeUnit timeUnit) {
        p.addValue((int) TimeUnit.MILLISECONDS.convert(duration, timeUnit));
    }

    /*Visible for testing*/NumerusRollingPercentile getPercentileHolder() {
        return p;
    }

    private void registerPercentileGauge(String metricName, String monitorId, Registry registry,
                                         final double percentile, String percentileTagValue) {
        Id id = registry.createId(metricName, "id", monitorId, "percentile", percentileTagValue);
        registry.gauge(id, p, value -> value.getPercentile(percentile));
    }
}
