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

package io.reactivex.netty.spectator;

import com.netflix.numerus.NumerusProperty;
import com.netflix.numerus.NumerusRollingPercentile;
import com.netflix.spectator.api.ExtendedRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.api.ValueFunction;

import java.util.concurrent.TimeUnit;

import static com.netflix.numerus.NumerusProperty.Factory.*;

/**
 * A latency metric for publishing various percentiles of latency which spectator does not.
 */
public class LatencyMetrics {

    private static final NumerusProperty<Integer> latency_timeInMilliseconds = asProperty(60000);
    private static final NumerusProperty<Integer> latency_numberOfBuckets = asProperty(12); // 12 buckets at 5000ms each
    private static final NumerusProperty<Integer> latency_bucketDataLength = asProperty(1000);
    private static final NumerusProperty<Boolean> latency_enabled = asProperty(true);

    private final NumerusRollingPercentile p;

    public LatencyMetrics(String metricName, String monitorId) {
        p = new NumerusRollingPercentile(latency_timeInMilliseconds, latency_numberOfBuckets, latency_bucketDataLength,
                                         latency_enabled);
        ExtendedRegistry registry = Spectator.registry();

        registerPercentileGauge(metricName, monitorId, registry, 5.0, "5");
        registerPercentileGauge(metricName, monitorId, registry, 25.0, "25");
        registerPercentileGauge(metricName, monitorId, registry, 50.0, "50");
        registerPercentileGauge(metricName, monitorId, registry, 75.0, "75");
        registerPercentileGauge(metricName, monitorId, registry, 90.0, "90");
        registerPercentileGauge(metricName, monitorId, registry, 99.0, "99");
        registerPercentileGauge(metricName, monitorId, registry, 99.5, "99_5");
        registerPercentileGauge(metricName, monitorId, registry, 100.0, "100");
        Id meanId = registry.createId(metricName, "id", monitorId, "percentile", "mean");
        registry.gauge(meanId, p, new ValueFunction() {
            @Override
            public double apply(Object ref) {
                return ((NumerusRollingPercentile) ref).getMean();
            }
        });
    }

    public void record(long duration, TimeUnit timeUnit) {
        p.addValue((int) TimeUnit.MILLISECONDS.convert(duration, timeUnit));
    }

    private void registerPercentileGauge(String metricName, String monitorId, ExtendedRegistry registry,
                                         final double percentile, String percentileTagValue) {
        Id id = registry.createId(metricName, "id", monitorId, "percentile", percentileTagValue);
        registry.gauge(id, p, new ValueFunction() {
            @Override
            public double apply(Object ref) {
                return ((NumerusRollingPercentile) ref).getPercentile(percentile);
            }
        });
    }
}
