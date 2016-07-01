package io.reactivex.netty.spectator.internal;

import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Meter;
import io.reactivex.netty.spectator.internal.LatencyMetrics.Percentile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class LatencyMetricTest {

    @Rule
    public final LatencyMetricRule metricRule = new LatencyMetricRule();

    @Test(timeout = 60000)
    public void testPercentiles() throws Exception {
        metricRule.populateMetrics();
        for (Percentile percentile : Percentile.values()) {
            Meter spectatorMetric = metricRule.getSpectatorMetric(percentile);
            double p = metricRule.metrics.getPercentileHolder().getPercentile(percentile.getValue());
            Iterator<Measurement> measure = spectatorMetric.measure().iterator();
            assertThat("Spectator metric measure is empty.", measure.hasNext());
            assertThat("Unexpected value of percentile: " + percentile.getTag(), measure.next().value(),
                       is(p));
        }
    }

    public static class LatencyMetricRule extends ExternalResource {

        private static final Random random = new Random();

        private LatencyMetrics metrics;
        private String monitorId;
        private String metricName;
        private DefaultRegistry registry;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    metricName = "latency-test";
                    monitorId = "latency-test-" + random.nextInt(100);
                    registry = new DefaultRegistry();
                    metrics = new LatencyMetrics(metricName, monitorId, registry);
                    base.evaluate();
                }
            };
        }

        public Meter getSpectatorMetric(Percentile percentile) {
            Id id = registry.createId(metricName, "id", monitorId, "percentile", percentile.getTag());
            return registry.get(id);
        }

        public void populateMetrics() {
            for (int i = 0; i < 100; i++) {
                metrics.record(1, TimeUnit.SECONDS);
            }
        }
    }
}
