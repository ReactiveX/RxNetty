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
package io.reactivex.netty.events;

import io.reactivex.netty.events.ListenersHolderTest.TestEventListener;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Subscription;

import java.util.Collection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ListenersHolderRule extends ExternalResource {

    private ListenersHolder<TestEventListener> holder;

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
        final TestEventListener listener = new TestEventListener();
        Subscription subscription = holder.subscribe(listener);
        assertListenerAdded(listener);

        return new ListenerWithSub(listener, subscription);
    }

    public void assertListenerAdded(TestEventListener... listeners) {
        Collection<TestEventListener> allListeners = holder.getAllListeners();
        assertThat("Unexpected listeners count in the holder.", allListeners, hasSize(listeners.length));
        assertThat("Listener not added to the holder.", allListeners, contains(listeners));
    }

    public ListenersHolder<TestEventListener> getHolder() {
        return holder;
    }

    public static class ListenerWithSub {

        final TestEventListener listener;
        final Subscription subscription;

        public ListenerWithSub(TestEventListener listener, Subscription subscription) {
            this.subscription = subscription;
            this.listener = listener;
        }

        public TestEventListener getListener() {
            return listener;
        }

        public Subscription getSubscription() {
            return subscription;
        }
    }
}
