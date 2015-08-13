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
package io.reactivex.netty.client;

import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.client.ConnectionObservable.AbstractOnSubscribeFunc;
import io.reactivex.netty.protocol.http.server.HttpServerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static io.reactivex.netty.test.util.DisabledEventPublisher.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ConnectionProviderTest {

    @Rule
    public final HttpServerRule serverRule = new HttpServerRule();

    @Rule
    public final ConnectionFactoryRule cfRule = new ConnectionFactoryRule();

    @Test(timeout = 60000)
    public void testForHost() throws Exception {

        ConnectionProvider<String, String> cf = serverRule.newConnectionFactoryForClient();

        ConnectionProvider<String, String> cfRealized = cfRule.start(cf);

        cfRule.connectAndAssert(cfRealized);
    }

    @Test(timeout = 60000)
    public void testCreate() throws Exception {
        ConnectionProvider<String, String> cf = cfRule.start();
        cfRule.connectAndAssert(cf);
    }

    public static class ConnectionFactoryRule extends ExternalResource {

        private ConnectionProvider<String, String> rawFactory;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    rawFactory = ConnectionProvider.create(
                            new Func1<ConnectionFactory<String, String>, ConnectionProvider<String, String>>() {
                                @Override
                                public ConnectionProvider<String, String> call(final ConnectionFactory<String, String> cf) {
                                    return new ConnectionProvider<String, String>(cf) {
                                        @Override
                                        public ConnectionObservable<String, String> nextConnection() {
                                            return cf.newConnection(new InetSocketAddress(0));
                                        }
                                    };
                                }
                            });
                    base.evaluate();
                }
            };
        }

        public <W, R> Connection<R, W> connectAndAssert(ConnectionProvider<W, R> connectionProvider) {
            TestSubscriber<Connection<R, W>> subscriber = new TestSubscriber<>();
            connectionProvider.nextConnection().subscribe(subscriber);

            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();

            assertThat("Unexpected number of connections returned.", subscriber.getOnNextEvents(), hasSize(1));
            assertThat("Unexpected connection returned.", subscriber.getOnNextEvents().get(0), is(not(nullValue())));

            return subscriber.getOnNextEvents().get(0);
        }

        public ConnectionProvider<String, String> start() {
            return start(rawFactory);
        }

        public <W, R> ConnectionProvider<W, R> start(ConnectionProvider<W, R> toRealize) {
            return realize(toRealize);
        }

        public <W, R> ConnectionProvider<W, R> realize(ConnectionProvider<W, R> toRealize) {

            return toRealize.realize(new ConnectionFactory<W, R>() {
                @Override
                public ConnectionObservable<R, W> newConnection(SocketAddress hostAddress) {

                    final Connection<R, W> c = ConnectionImpl.create(new EmbeddedChannel(), null,
                                                                     DISABLED_EVENT_PUBLISHER);
                    return ConnectionObservable.createNew(new AbstractOnSubscribeFunc<R, W>() {
                        @Override
                        public void doSubscribe(Subscriber<? super Connection<R, W>> subscriber,
                                                Action1<ConnectionObservable<R, W>> subscribeAllListenersAction) {
                            subscriber.onNext(c);
                            subscriber.onCompleted();
                        }
                    });
                }
            });
        }
    }
}