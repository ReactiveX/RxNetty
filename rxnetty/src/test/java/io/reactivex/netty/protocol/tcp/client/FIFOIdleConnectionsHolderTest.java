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
package io.reactivex.netty.protocol.tcp.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import rx.observers.TestSubscriber;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class FIFOIdleConnectionsHolderTest {

    @Rule
    public final HolderRule holderRule = new HolderRule();

    @Test
    public void testPoll() throws Exception {
        holderRule.pollNow();

        @SuppressWarnings("unchecked")
        final PooledConnection<String, String> mock = Mockito.mock(PooledConnection.class);
        Mockito.when(mock.isUsable()).thenReturn(true);

        PooledConnection<String, String> added = holderRule.addAConnection();

        holderRule.pollNow(added);

        holderRule.pollNow(); // Poll removes the item.
    }

    @Test
    public void testPeek() throws Exception {
        holderRule.peekNow();

        @SuppressWarnings("unchecked")
        final PooledConnection<String, String> mock = Mockito.mock(PooledConnection.class);
        Mockito.when(mock.isUsable()).thenReturn(true);

        PooledConnection<String, String> added = holderRule.addAConnection();

        holderRule.peekNow(added);

        holderRule.peekNow(added); // Peek does not removes the item.
    }

    @Test
    public void testAdd() throws Exception {
        PooledConnection<String, String> added = holderRule.addAConnection();

        holderRule.peekNow(added);

        PooledConnection<String, String> added2 = holderRule.addAConnection();

        holderRule.peekNow(added, added2); // Get both items in the same order.
    }

    @Test
    public void testRemove() throws Exception {
        PooledConnection<String, String> added = holderRule.addAConnection();
        PooledConnection<String, String> added2 = holderRule.addAConnection();

        holderRule.peekNow(added, added2); // Get both items in the same order.

        holderRule.holder.remove(added);

        holderRule.peekNow(added2); // one item is removed
    }

    @Test
    public void testDoCopy() throws Exception {
        PooledConnection<String, String> added = holderRule.addAConnection();
        holderRule.peekNow(added); // check if added.

        IdleConnectionsHolder<String, String> newHolder = holderRule.holder.doCopy(null);
        HolderRule.peekNow(newHolder); // Copy should not copy state.
    }

    public static class HolderRule extends ExternalResource {

        private FIFOIdleConnectionsHolder<String, String> holder;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    holder = new FIFOIdleConnectionsHolder<>();
                    base.evaluate();
                }
            };
        }

        @SafeVarargs
        public final TestSubscriber<PooledConnection<String, String>> pollNow(PooledConnection<String, String>... expected) {
            TestSubscriber<PooledConnection<String, String>> subscriber = new TestSubscriber<>();
            holder.poll().subscribe(subscriber);

            subscriber.assertNoErrors();
            subscriber.assertTerminalEvent();

            assertThat("Unexpected connections received from the holder.", subscriber.getOnNextEvents(),
                       hasSize(expected.length));

            if (expected.length > 0) {
                assertThat("Unexpected connections received from the holder.", subscriber.getOnNextEvents(),
                           contains(expected));
            }

            return subscriber;
        }

        @SafeVarargs
        public final TestSubscriber<PooledConnection<String, String>> peekNow(PooledConnection<String, String>... expected) {
            return peekNow(holder, expected);
        }

        @SafeVarargs
        public static TestSubscriber<PooledConnection<String, String>> peekNow(
                IdleConnectionsHolder<String, String> holder,
                PooledConnection<String, String>... expected) {
            TestSubscriber<PooledConnection<String, String>> subscriber = new TestSubscriber<>();
            holder.peek().subscribe(subscriber);

            subscriber.assertNoErrors();
            subscriber.assertTerminalEvent();

            assertThat("Unexpected connections received from the holder.", subscriber.getOnNextEvents(),
                       hasSize(expected.length));

            if (expected.length > 0) {
                assertThat("Unexpected connections received from the holder.", subscriber.getOnNextEvents(),
                           contains(expected));
            }

            return subscriber;
        }

        public PooledConnection<String, String> addAConnection() {

            @SuppressWarnings("unchecked")
            PooledConnection<String, String> mock = Mockito.mock(PooledConnection.class);
            Mockito.when(mock.isUsable()).thenReturn(true);

            holder.add(mock);

            return mock;
        }
    }
}