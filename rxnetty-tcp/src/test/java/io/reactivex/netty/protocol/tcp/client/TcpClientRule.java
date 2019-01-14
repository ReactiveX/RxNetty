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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.pool.PooledConnection;
import io.reactivex.netty.client.pool.PooledConnectionProvider;
import io.reactivex.netty.client.pool.SingleHostPoolingProviderFactory;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.Observer;
import rx.functions.Func0;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TcpClientRule extends ExternalResource {

    private TcpServer<ByteBuf, ByteBuf> server;
    private TcpClient<ByteBuf, ByteBuf> client;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                server = TcpServer.newServer();
                base.evaluate();
            }
        };
    }

    public void startServer(int maxConnections) {
        server.start(new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(Connection<ByteBuf, ByteBuf> newConnection) {
                return newConnection.writeAndFlushOnEach(newConnection.getInput());
            }
        });
        createClient(maxConnections);
    }

    public void startServer(ConnectionHandler<ByteBuf, ByteBuf> handler, int maxConnections) {
        server.start(handler);
        createClient(maxConnections);
    }

    public PooledConnection<ByteBuf, ByteBuf> connect() {

        TestSubscriber<Connection<ByteBuf, ByteBuf>> cSub = new TestSubscriber<>();
        client.createConnectionRequest().subscribe(cSub);

        cSub.awaitTerminalEvent();

        cSub.assertNoErrors();

        assertThat("No connection received.", cSub.getOnNextEvents(), hasSize(1));

        return  (PooledConnection<ByteBuf, ByteBuf>) cSub.getOnNextEvents().get(0);
    }

    public PooledConnection<ByteBuf, ByteBuf> connectWithCheck() {

        final AtomicBoolean gotOnNext = new AtomicBoolean(false);

        Observable<Connection<ByteBuf, ByteBuf>> got_no_connection = client.createConnectionRequest()
            .doOnEach(new Observer<Connection<ByteBuf, ByteBuf>>() {
                @Override
                public void onCompleted() {
                    if(!gotOnNext.get()) {
                        //A PooledConnection could sometimes send onCompleted before the onNext event occurred.
                        Assert.fail("Should not get onCompletedBefore onNext");
                    }
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onNext(Connection<ByteBuf, ByteBuf> byteBufByteBufConnection) {
                    gotOnNext.set(true);
                }
            })
            .switchIfEmpty(Observable.defer(new Func0<Observable<PooledConnection<ByteBuf, ByteBuf>>>() {
                @Override
                public Observable<PooledConnection<ByteBuf, ByteBuf>> call() {
                    return Observable.empty();
                }
            }));

        TestSubscriber<Connection<ByteBuf, ByteBuf>> cSub = new TestSubscriber<>();
        got_no_connection.subscribe(cSub);

        cSub.awaitTerminalEvent();

        cSub.assertNoErrors();

        assertThat("No connection received.", cSub.getOnNextEvents(), hasSize(1));

        return  (PooledConnection<ByteBuf, ByteBuf>) cSub.getOnNextEvents().get(0);
    }

    private void createClient(final int maxConnections) {
        InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", server.getServerPort());
        client = TcpClient.newClient(SingleHostPoolingProviderFactory.<ByteBuf, ByteBuf>createBounded(maxConnections),
                                     Observable.just(new Host(serverAddr)));
    }

    public TcpServer<ByteBuf, ByteBuf> getServer() {
        return server;
    }

    public TcpClient<ByteBuf, ByteBuf> getClient() {
        return client;
    }
}
