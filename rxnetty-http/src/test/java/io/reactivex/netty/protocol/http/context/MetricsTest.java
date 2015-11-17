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

package io.reactivex.netty.protocol.http.context;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.reactivex.netty.client.pool.CompositePoolLimitDeterminationStrategy;
import io.reactivex.netty.client.pool.MaxConnectionsBasedStrategy;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.PooledConnectionProvider;
import io.reactivex.netty.contexts.ThreadLocalRequestCorrelator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.spectator.http.HttpClientListener;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MetricsTest {
    private static final String REQUEST_ID_HEADER_NAME = "request_id";
    private static final ThreadLocalRequestCorrelator CORRELATOR = new ThreadLocalRequestCorrelator();

    @Test
    public void testConnectionPoolMetrics() throws InterruptedException, ExecutionException, TimeoutException {
        final PoolConfig<ByteBuf, ByteBuf> config = new PoolConfig<>();
        config.limitDeterminationStrategy(new CompositePoolLimitDeterminationStrategy(
            new MaxConnectionsBasedStrategy(100), new MaxConnectionsBasedStrategy(100)));

        HttpClient<ByteBuf, ByteBuf> client = HttpClient
            .newClient(PooledConnectionProvider.create(config, new InetSocketAddress("www.google.com", 80)))
            .<ByteBuf, ByteBuf>addChannelHandlerLast("aggregator", new Func0<ChannelHandler>() {
                @Override
                public ChannelHandler call() {
                    return new HttpObjectAggregator(1024*1024);
                }
            }).context(REQUEST_ID_HEADER_NAME, CORRELATOR);

        HttpClientListener listener = new HttpClientListener("default");
        client.subscribe(listener);

        client.createGet("/").flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>(){
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
