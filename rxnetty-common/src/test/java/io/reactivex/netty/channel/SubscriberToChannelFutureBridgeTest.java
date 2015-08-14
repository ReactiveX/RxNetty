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

package io.reactivex.netty.channel;

import io.netty.channel.ChannelFuture;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import rx.observers.TestSubscriber;

public class SubscriberToChannelFutureBridgeTest {

    @Rule
    public final BridgeRule rule = new BridgeRule();

    @Test(timeout = 60000)
    public void testBridge() throws Exception {

        rule.subscriber.unsubscribe();

        Mockito.verify(rule.future).addListener(rule.bridge);
        Mockito.verify(rule.future).removeListener(rule.bridge);
    }

    @Test(timeout = 60000)
    public void testSuccess() throws Exception {
        rule.completeFuture();
        rule.subscriber.assertTerminalEvent();
        rule.subscriber.assertNoErrors();
    }

    @Test(timeout = 60000)
    public void testError() throws Exception {
        rule.failFuture();
        rule.subscriber.assertTerminalEvent();
        rule.subscriber.assertError(IllegalStateException.class);
    }

    public static class BridgeRule extends ExternalResource {

        public TestSubscriber<Void> subscriber;
        public SubscriberToChannelFutureBridge bridge;
        private ChannelFuture future;
        private volatile boolean futureTerminated;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    subscriber = new TestSubscriber<>();
                    bridge = new SubscriberToChannelFutureBridge() {

                        @Override
                        protected void doOnSuccess(ChannelFuture future) {
                            subscriber.onCompleted();
                        }

                        @Override
                        protected void doOnFailure(ChannelFuture future, Throwable cause) {
                            subscriber.onError(cause);
                        }
                    };

                    future = Mockito.mock(ChannelFuture.class);

                    bridge.bridge(future, subscriber);
                    base.evaluate();
                }
            };
        }

        public void completeFuture() throws Exception {
            if (futureTerminated) {
                throw new IllegalStateException("Channel future is already terminated");
            }
            futureTerminated = true;
            Mockito.when(future.isSuccess()).thenReturn(true);
            bridge.operationComplete(future);
        }

        public void failFuture() throws Exception {
            if (futureTerminated) {
                throw new IllegalStateException("Channel future is already terminated");
            }
            futureTerminated = true;
            Mockito.when(future.isSuccess()).thenReturn(false);
            Mockito.when(future.cause()).thenReturn(new IllegalStateException("Force terminate"));
            bridge.operationComplete(future);
        }
    }

}