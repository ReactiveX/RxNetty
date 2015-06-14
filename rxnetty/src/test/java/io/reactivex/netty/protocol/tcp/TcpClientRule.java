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
package io.reactivex.netty.protocol.tcp;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.pool.PooledConnection;
import io.reactivex.netty.channel.pool.PooledConnectionProvider;
import io.reactivex.netty.protocol.tcp.client.ConnectionProvider;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import io.reactivex.netty.protocol.tcp.server.ConnectionHandler;
import io.reactivex.netty.protocol.tcp.server.TcpServer;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;

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
        InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", server.getServerPort());
        createClient(PooledConnectionProvider.<ByteBuf, ByteBuf>createBounded(maxConnections, serverAddr));
    }

    public void startServer(ConnectionHandler<ByteBuf, ByteBuf> handler, int maxConnections) {
        server.start(handler);
        InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", server.getServerPort());
        createClient(PooledConnectionProvider.<ByteBuf, ByteBuf>createBounded(maxConnections, serverAddr));
    }

    public PooledConnection<ByteBuf, ByteBuf> connect() {

        TestSubscriber<Connection<ByteBuf, ByteBuf>> cSub = new TestSubscriber<>();
        client.createConnectionRequest().subscribe(cSub);

        cSub.awaitTerminalEvent();

        cSub.assertNoErrors();

        assertThat("No connection received.", cSub.getOnNextEvents(), hasSize(1));

        return  (PooledConnection<ByteBuf, ByteBuf>) cSub.getOnNextEvents().get(0);
    }

    protected void createClient(ConnectionProvider<ByteBuf, ByteBuf> connectionProvider) {
        client = TcpClient.newClient(connectionProvider);
    }

    public TcpServer<ByteBuf, ByteBuf> getServer() {
        return server;
    }

    public TcpClient<ByteBuf, ByteBuf> getClient() {
        return client;
    }
}
