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
package io.reactivex.netty.events;

import io.reactivex.netty.test.util.MockEventListener;
import org.hamcrest.Matchers;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Subscription;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.*;

public class ListenersHolderRule extends ExternalResource {

    private ListenersHolder<MockEventListener> holder;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                holder = new ListenersHolder<>();
                base.evaluate();
            }
        };
    }

    public ListenerWithSub addAListener() {
        final MockEventListener listener = new MockEventListener();
        Subscription subscription = holder.subscribe(listener);
        assertListenerAdded(listener);

        return new ListenerWithSub(listener, subscription);
    }

    public void assertListenerAdded(MockEventListener... listeners) {
        Collection<MockEventListener> allListeners = holder.getAllListeners();
        assertThat("Unexpected listeners count in the holder.", allListeners, Matchers.hasSize(listeners.length));
        assertThat("Listener not added to the holder.", allListeners, Matchers.contains(listeners));
    }

    public ListenersHolder<MockEventListener> getHolder() {
        return holder;
    }

    public static class ListenerWithSub {

        final MockEventListener listener;
        final Subscription subscription;

        public ListenerWithSub(MockEventListener listener, Subscription subscription) {
            this.subscription = subscription;
            this.listener = listener;
        }

        public MockEventListener getListener() {
            return listener;
        }

        public Subscription getSubscription() {
            return subscription;
        }
    }
}
