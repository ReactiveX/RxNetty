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

package io.reactivex.netty.client.loadbalancer;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class LoadBalancerFactoryTest {

    @Rule
    public final LBFactoryRule rule = new LBFactoryRule();

    @Test(timeout = 60000)
    public void testHostRemove() throws Exception {

        TestSubscriber<List<HostHolder<String, String>>> testSubscriber = rule.newHostsListListener();

        rule.initProvider();
        Host host = rule.emitHost();
        testSubscriber.assertNoTerminalEvent();

        assertThat("Unexpected number of hosts lists received", testSubscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected number of hosts received", testSubscriber.getOnNextEvents().get(0), hasSize(1));
        assertThat("Unexpected host received", testSubscriber.getOnNextEvents().get(0).get(0).getConnector().getHost(),
                   is(host));

        rule.completeHost(host);

        assertThat("Unexpected number of hosts lists received", testSubscriber.getOnNextEvents(), hasSize(2));
        assertThat("Unexpected number of hosts received", testSubscriber.getOnNextEvents().get(1),
                   is(Matchers.<HostHolder<String,String>>empty()));
    }

    @Test(timeout = 60000)
    public void testDuplicateHost() throws Exception {
        TestSubscriber<List<HostHolder<String, String>>> testSubscriber = rule.newHostsListListener();

        rule.initProvider();
        Host host = rule.emitHost();
        testSubscriber.assertNoTerminalEvent();

        assertThat("Unexpected number of hosts lists received", testSubscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected number of hosts received", testSubscriber.getOnNextEvents().get(0), hasSize(1));
        assertThat("Unexpected host received", testSubscriber.getOnNextEvents().get(0).get(0).getConnector().getHost(),
                   is(host));

        rule.emitHost(host);
        testSubscriber.assertNoTerminalEvent();

        assertThat("Unexpected number of hosts lists received", testSubscriber.getOnNextEvents(), hasSize(1));
        assertThat("Unexpected number of hosts received", testSubscriber.getOnNextEvents().get(0), hasSize(1));
        assertThat("Unexpected host received", testSubscriber.getOnNextEvents().get(0).get(0).getConnector().getHost(),
                   is(host));
    }

    @Test(timeout = 60000)
    public void testHostSourceEmitsError() throws Exception {
        TestSubscriber<List<HostHolder<String, String>>> testSubscriber = rule.newHostsListListener();

        rule.initProvider();
        rule.emitHost();
        testSubscriber.assertNoTerminalEvent();

        assertThat("Unexpected number of hosts lists received", testSubscriber.getOnNextEvents(), hasSize(1));

        rule.hostStream.onError(new NullPointerException("Deliberate exception"));

        testSubscriber.assertNoTerminalEvent();
        assertThat("Unexpected number of hosts lists received", testSubscriber.getOnNextEvents(), hasSize(1));
    }

    public static class LBFactoryRule extends ExternalResource {

        private PublishSubject<List<HostHolder<String, String>>> lists = PublishSubject.create();
        private PublishSubject<Host> hostStream = PublishSubject.create();
        private List<Host> emittedHosts = new ArrayList<>();
        private ConnectionProvider<String, String> connectionProviderMock;
        private EventSource<ClientEventListener> eventSourceMock;
        private EventPublisher eventPublisherMock;
        private ClientEventListener eventListenerMock;
        private LoadBalancerFactory<String, String> factory;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    @SuppressWarnings("unchecked")
                    ConnectionProvider<String, String> m =
                            (ConnectionProvider<String, String>) Mockito.mock(ConnectionProvider.class);
                    connectionProviderMock = m;

                    @SuppressWarnings("unchecked")
                    EventSource<ClientEventListener> es = (EventSource<ClientEventListener>)Mockito.mock(EventSource.class);
                    eventSourceMock = es;

                    eventPublisherMock = Mockito.mock(EventPublisher.class);
                    eventListenerMock = Mockito.mock(ClientEventListener.class);

                    factory = LoadBalancerFactory.create(new LoadBalancingStrategy<String, String>() {
                       @Override
                       public ConnectionProvider<String, String> newStrategy(List<HostHolder<String, String>> hosts) {
                           lists.onNext(hosts);
                           return new ConnectionProvider<String, String>() {
                               @Override
                               public Observable<Connection<String, String>> newConnectionRequest() {
                                   return Observable.empty();
                               }
                           };
                       }

                       @Override
                       public HostHolder<String, String> toHolder(HostConnector<String, String> connector) {
                           return new HostHolder<>(connector, eventListenerMock);
                       }
                   });
                    base.evaluate();
                }
            };
        }

        public ConnectionProvider<String, String> initProvider() {
            return factory.newProvider(hostStream.map(new Func1<Host, HostConnector<String, String>>() {
                @Override
                public HostConnector<String, String> call(Host host) {
                    return new HostConnector<>(host, connectionProviderMock, eventSourceMock, eventPublisherMock,
                                               eventListenerMock);
                }
            }));
        }

        public TestSubscriber<List<HostHolder<String, String>>> newHostsListListener() {
            TestSubscriber<List<HostHolder<String, String>>> testSubscriber = new TestSubscriber<>();
            lists.subscribe(testSubscriber);
            return testSubscriber;
        }

        public Host emitHost() {
            Host host = new Host(new InetSocketAddress(0), PublishSubject.<Void>create());
            emittedHosts.add(host);
            hostStream.onNext(host);
            return host;
        }

        public void emitHost(Host host) {
            emittedHosts.add(host);
            hostStream.onNext(host);
        }

        public void completeHost(int index) {
            completeHost(emittedHosts.get(index));
        }

        public void completeHost(Host host) {
            ((PublishSubject<Void>)host.getCloseNotifier()).onCompleted();
        }
    }
}