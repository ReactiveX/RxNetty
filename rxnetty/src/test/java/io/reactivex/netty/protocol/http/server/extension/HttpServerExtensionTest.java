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

package io.reactivex.netty.protocol.http.server.extension;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpConnectionHandler;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerMetricsEvent;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

/**
 * Tests that checks functionality of extended {@link io.reactivex.netty.protocol.http.server.HttpServer} and
 * {@link io.reactivex.netty.protocol.http.server.HttpServerBuilder}.
 * <p/>
 * Note: This tests need to be in other package than {@code io.reactivex.netty.protocol.http.server} because
 * they also test visibility of {@link io.reactivex.netty.protocol.http.server.HttpConnectionHandler}.
 *
 * @author Michal Gajdos
 */
public class HttpServerExtensionTest {

    private static class MyHttpServer extends HttpServer<ByteBuf, ByteBuf> {

        private final AtomicInteger startCount = new AtomicInteger(0);
        private final AtomicInteger shutdownCount = new AtomicInteger(0);

        private MyHttpServer(final ServerBootstrap bootstrap,
                             final int port,
                             final PipelineConfigurator<HttpServerRequest<ByteBuf>, HttpServerResponse<ByteBuf>> configurator,
                             final HttpConnectionHandler<ByteBuf, ByteBuf> connectionHandler,
                             final EventExecutorGroup requestProcessingExecutor,
                             final long requestContentSubscriptionTimeoutMs) {
            super(bootstrap, port, configurator, connectionHandler, requestProcessingExecutor,
                    requestContentSubscriptionTimeoutMs);
        }

        @Override
        public HttpServer<ByteBuf, ByteBuf> start() {
            super.start();

            // Test hook.
            startCount.incrementAndGet();

            return this;
        }

        @Override
        public void shutdown() throws InterruptedException {
            // Test hook.
            shutdownCount.incrementAndGet();

            super.shutdown();
        }
    }

    private static class MyHttpServerBuilder extends HttpServerBuilder<ByteBuf, ByteBuf> {

        MyHttpServerBuilder(final int port, final RequestHandler<ByteBuf, ByteBuf> requestHandler) {
            super(port, requestHandler);
        }

        @Override
        protected HttpServer<ByteBuf, ByteBuf> createServer() {
            return new MyHttpServer(serverBootstrap, port, pipelineConfigurator,
                    (HttpConnectionHandler<ByteBuf, ByteBuf>) connectionHandler,
                    eventExecutorGroup, contentSubscriptionTimeoutMs);
        }

        @Override
        protected MetricEventsListener<ServerMetricsEvent<?>>
        newMetricsListener(final MetricEventsListenerFactory factory,
                           final RxServer<HttpServerRequest<ByteBuf>, HttpServerResponse<ByteBuf>> server) {
            return super.newMetricsListener(factory, server);
        }
    }

    /**
     * Test that it's possible to extend server and server builder and add some "hooks" to
     * HttpServer's start and shutdown methods.
     */
    @Test
    public void testStartAndShutdownServerHooks() throws Exception {
        final HttpServerBuilder<ByteBuf, ByteBuf> builder = new MyHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(final HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                return response.writeStringAndFlush("OK");
            }
        });
        final MyHttpServer server = (MyHttpServer) builder.build();

        try {
            server.start();
            assertEquals("Server start hook wasn't invoked.", 1, server.startCount.get());
            assertEquals("Server shutdown hook invoked unexpectedly.", 0, server.shutdownCount.get());

            final CountDownLatch finishLatch = new CountDownLatch(1);
            final String response = RxNetty.createHttpClient("localhost", server.getServerPort())
                    .submit(HttpClientRequest.createGet("/"))
                    .finallyDo(new Action0() {
                        @Override
                        public void call() {
                            finishLatch.countDown();
                        }
                    })
                    .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                        @Override
                        public Observable<ByteBuf> call(final HttpClientResponse<ByteBuf> response) {
                            return response.getContent();
                        }
                    })
                    .map(new Func1<ByteBuf, String>() {
                        @Override
                        public String call(final ByteBuf byteBuf) {
                            return byteBuf.toString(Charset.forName("UTF-8"));
                        }
                    })
                    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);

            assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
            assertEquals("Returned entity should be 'OK'.", "OK", response);
        } finally {
            server.shutdown();
        }

        assertEquals("Server start hook invoked unexpectedly.", 1, server.startCount.get());
        assertEquals("Server shutdown hook wasn't invoked.", 1, server.shutdownCount.get());
    }
}
