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
 */
package io.reactivex.netty.client;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.ssl.DefaultFactories;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class SslClientTest {

    @Test
    public void testReleaseOnSslFailure() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return Observable.empty();
            }
        }).build().start();

        final MaxConnectionsBasedStrategy strategy = new MaxConnectionsBasedStrategy(1);
        try {
            // The connect fails because the server does not support SSL.
            RxNetty.<ByteBuf, ByteBuf>newTcpClientBuilder("localhost", server.getServerPort())
                   .withConnectionPoolLimitStrategy(strategy)
                   .withSslEngineFactory(DefaultFactories.trustAll())
                   .build().connect().toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            if (!(e.getCause() instanceof SSLException)) {
                throw e;
            }
        }

        Assert.assertEquals("Unexpected available permits.", 1, strategy.getAvailablePermits());
    }
}
