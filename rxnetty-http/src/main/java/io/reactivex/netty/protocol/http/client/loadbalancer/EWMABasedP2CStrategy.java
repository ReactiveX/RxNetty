/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.protocol.http.client.loadbalancer;

import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.client.loadbalancer.AbstractP2CStrategy;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.http.client.loadbalancer.EWMABasedP2CStrategy.HttpClientListenerImpl;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class EWMABasedP2CStrategy<W, R> extends AbstractP2CStrategy<W, R, ClientEventListener> {
    private static final double STARTUP_PENALTY = Long.MAX_VALUE >> 12;
    private final double tauUp;
    private final double tauDown;
    private double penaltyOnConnectionFailure;
    private double penaltyOn503;

    public EWMABasedP2CStrategy(double tauUp, double tauDown, double penaltyOnConnectionFailure,
                                double penaltyOn503) {
        this.tauUp = tauUp;
        this.tauDown = tauDown;
        this.penaltyOnConnectionFailure = penaltyOnConnectionFailure;
        this.penaltyOn503 = penaltyOn503;
    }

    public EWMABasedP2CStrategy() {
        this(NANOSECONDS.convert(1, SECONDS), NANOSECONDS.convert(15, SECONDS), 2, 5);
    }

    @Override
    protected HttpClientListenerImpl newListener(Host host) {
        return new HttpClientListenerImpl();
    }

    @Override
    protected double getWeight(ClientEventListener listener) {
        return ((HttpClientListenerImpl) listener).getWeight();
    }

    public class HttpClientListenerImpl extends HttpClientEventsListener {
        private final long epoch = System.nanoTime();
        private long stamp = epoch;  // last timestamp in nanos we observed an rtt
        private int pending = 0;     // instantaneous rate
        private double cost = 0.0;   // ewma of rtt, sensitive to peaks.

        public double getWeight() {
            observe(0.0);
            if (cost == 0.0 && pending != 0) {
                return STARTUP_PENALTY + pending;
            } else {
                return cost * (pending+1);
            }
        }

        @Override
        public synchronized void onRequestWriteComplete(long duration, TimeUnit timeUnit) {
            pending += 1;
        }

        @Override
        public synchronized void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {
            pending -= 1;
            observe(NANOSECONDS.convert(duration, timeUnit));
        }

        @Override
        public void onResponseHeadersReceived(int responseCode, long duration, TimeUnit timeUnit) {
            if (responseCode == 503) {
                observe(TimeUnit.NANOSECONDS.convert(duration, timeUnit) * penaltyOn503);
            }
        }

        @Override
        public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            observe(TimeUnit.NANOSECONDS.convert(duration, timeUnit) * penaltyOnConnectionFailure);
        }

        private void observe(double rtt) {
            long t = System.nanoTime();
            long td = Math.max(t - stamp, 0L);
            if (rtt > cost) {
                double w = Math.exp(-td / tauUp);
                cost = cost * w + rtt * (1.0 - w);
            } else {
                double w = Math.exp(-td / tauDown);
                cost = cost * w + rtt * (1.0 - w);
            }
            stamp = t;
        }
    }
}
