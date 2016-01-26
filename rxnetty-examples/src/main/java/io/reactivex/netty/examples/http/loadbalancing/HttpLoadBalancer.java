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
package io.reactivex.netty.examples.http.loadbalancing;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.examples.tcp.loadbalancing.AbstractLoadBalancer;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * This is an implementation of {@link AbstractLoadBalancer} for HTTP clients.
 *
 * This load balancer uses a naive failure detector that removes the host on the first 503 HTTP response. The
 * intention here is just to demonstrate how to write complex load balancing logic using lower level constructs in the
 * client.
 *
 * @param <W> Type of Objects written on the connections created by this load balancer.
 * @param <R> Type of Objects read from the connections created by this load balancer.
 *
 * @see AbstractLoadBalancer
 * @see HttpLoadBalancingClient
 */
public class HttpLoadBalancer<W, R> extends AbstractLoadBalancer<W, R> {

    private static final Logger logger = LoggerFactory.getLogger(HttpLoadBalancer.class);

    public HttpLoadBalancer() {
        super();
    }

    @Override
    protected ClientEventListener newListener() {
        return new ClientEventListenerImpl();
    }

    @Override
    protected long getWeight(ClientEventListener eventListener) {
        return ((ClientEventListenerImpl)eventListener).weight;
    }

    private static class ClientEventListenerImpl extends HttpClientEventsListener {

        private volatile long weight = Long.MAX_VALUE;

        @Override
        public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            weight = Long.MIN_VALUE;
        }

        @Override
        public void onByteRead(long bytesRead) {
            super.onByteRead(bytesRead);
        }

        @Override
        public void onResponseHeadersReceived(int responseCode, long duration, TimeUnit timeUnit) {
            logger.error("Response code: " + responseCode);
            if (HttpResponseStatus.SERVICE_UNAVAILABLE.code() == responseCode) {
                weight = Long.MIN_VALUE;
            }
        }
    }
}
