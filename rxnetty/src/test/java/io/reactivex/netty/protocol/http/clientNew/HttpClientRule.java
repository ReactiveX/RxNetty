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
package io.reactivex.netty.protocol.http.clientNew;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ClientEmbeddedConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ClientState;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.test.util.InboundRequestFeeder;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.functions.Func0;
import rx.functions.Func1;

public class HttpClientRule extends ExternalResource {

    private HttpClient<ByteBuf, ByteBuf> httpClient;
    private EmbeddedChannel channel;
    private TcpClient<ByteBuf, ByteBuf> tcpClient;
    private InboundRequestFeeder inboundRequestFeeder;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                inboundRequestFeeder = new InboundRequestFeeder();
                Func1<ClientState<ByteBuf, ByteBuf>, ClientConnectionFactory<ByteBuf, ByteBuf>> factory =
                        ClientEmbeddedConnectionFactory.newFactoryFunc(new Func0<EmbeddedChannel>() {
                            @Override
                            public EmbeddedChannel call() {
                                channel = new EmbeddedChannel(inboundRequestFeeder);
                                return channel;
                            }
                        });

                tcpClient = TcpClient.newClient("localhost", 0)
                                     .connectionFactory(factory)
                                     .enableWireLogging(LogLevel.ERROR);
                httpClient = HttpClientImpl.unsafeCreate(tcpClient);
                base.evaluate();
            }
        };
    }

    public HttpClient<ByteBuf, ByteBuf> getHttpClient() {
        return httpClient;
    }

    public EmbeddedChannel getChannel() {
        return channel;
    }

    public TcpClient<ByteBuf, ByteBuf> getTcpClient() {
        return tcpClient;
    }

    public void feedResponse(HttpContent... content) {
        inboundRequestFeeder.addToTheFeed((Object)content);
    }

    public void feedResponse(HttpResponse response, HttpContent content) {
        inboundRequestFeeder.addToTheFeed(response, content);
    }

    public void feedResponseAndComplete(HttpResponse response, HttpContent content) {
        inboundRequestFeeder.addToTheFeed(response, content, new DefaultLastHttpContent());
    }

    public void feedResponseAndComplete(HttpResponse response) {
        inboundRequestFeeder.addToTheFeed(response, new DefaultLastHttpContent());
    }
}
