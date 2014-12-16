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
package io.reactivex.netty.protocol.http.clientNew;

import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import rx.functions.Action1;

import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class HttpClientImpl<I, O> extends HttpClient<I, O> {

    private final String host;
    private final int port;

    public HttpClientImpl() {
        this(InetAddress.getLoopbackAddress().getHostName(), 80);
    }

    public HttpClientImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public HttpClientRequest<I, O> createGet(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createPost(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createPut(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createDelete(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createHead(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createOptions(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createPatch(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createTrace(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClientRequest<I, O> createConnect(String uri) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> readTimeOut(int timeOut, TimeUnit timeUnit) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> followRedirects(int maxRedirects) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> followRedirects(boolean follow) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <T> HttpClient<I, O> channelOption(ChannelOption<T> option, T value) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public <II, OO> HttpClient<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> eventLoop(EventLoopGroup eventLoopGroup) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> maxConnections(int maxConnections) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> withIdleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> withPoolIdleCleanupScheduler(ScheduledExecutorService poolIdleCleanupScheduler) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> withNoIdleConnectionCleanup() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> withNoConnectionPooling() {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> enableWireLogging(LogLevel wireLogginLevel) {
        // TODO: Auto-generated method stub
        return null;
    }

    @Override
    public HttpClient<I, O> withSslEngineFactory(SSLEngineFactory sslEngineFactory) {
        // TODO: Auto-generated method stub
        return null;
    }
}
