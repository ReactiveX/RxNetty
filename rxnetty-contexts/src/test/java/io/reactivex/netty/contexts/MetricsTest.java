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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.client.CompositePoolLimitDeterminationStrategy;
import io.reactivex.netty.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.servo.http.HttpClientListener;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Nitesh Kant
 */
public class MetricsTest {

    @Test
    public void testConnectionPoolMetrics() throws InterruptedException, ExecutionException, TimeoutException {
        HttpClientBuilder<ByteBuf, ByteBuf> builder = RxContexts.newHttpClientBuilder("www.google.com", 80, RxContexts.DEFAULT_CORRELATOR);
        HttpClient<ByteBuf, ByteBuf> client = builder.withConnectionPoolLimitStrategy(new CompositePoolLimitDeterminationStrategy(
                new MaxConnectionsBasedStrategy(100), new MaxConnectionsBasedStrategy(100))).build();
        HttpClientListener listener = HttpClientListener.newHttpListener("default");
        client.subscribe(listener);
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/");
        client.submit(request).flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>(){
            @Override
            public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> t1) {
                return t1.getContent();
            }
        }).map(new Func1<ByteBuf, String>(){
            @Override
            public String call(ByteBuf t1) {
                return t1.toString(Charset.defaultCharset());
            }
        }).toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        Assert.assertEquals("Unexpected connection count.", 1, listener.getConnectionCount());
        Assert.assertEquals("Unexpected pool acquire count.", 1, listener.getPoolAcquires());
    }
}
