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
package io.reactivex.netty.channel;

import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.observers.Subscribers;
import rx.subscriptions.Subscriptions;

/**
 * An event to communicate the subscriber of the associated connection input stream created by
 * {@link io.reactivex.netty.channel.AbstractConnectionToChannelBridge}.
 *
 * <h2>Multiple events on the same channel</h2>
 *
 * Multiple instance of this event can be sent on the same channel, provided that there is a
 * {@link ConnectionInputSubscriberResetEvent} between two consecutive {@link ConnectionInputSubscriberEvent}s
 *
 * @param <R> Type read from the connection held by the event.
 * @param <W> Type written to the connection held by the event.
 */
public final class ConnectionInputSubscriberEvent<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionInputSubscriberEvent.class);

    private final Subscriber<? super R> subscriber;

    public ConnectionInputSubscriberEvent(Subscriber<? super R> subscriber, final Connection<R, W> connection) {
        this.subscriber = subscriber;
        this.subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                // Unsubscribe from the input closes the connection as there can only be one subscriber to the
                // input and, if nothing is read, it means, nobody is using the connection.
                // For fire-and-forget usecases, one should explicitly ignore content on the connection which
                // adds a discard all subscriber that never unsubscribes. For this case, then, the close becomes
                // explicit.
                connection.close()
                          .subscribe(Actions.empty(), new Action1<Throwable>() {
                              @Override
                              public void call(Throwable throwable) {
                                  logger.error("Failed to close the connection.", throwable);
                              }
                          });
            }
        }));
    }

    public Subscriber<? super R> getSubscriber() {
        return subscriber;
    }

    public static <II, OO> ConnectionInputSubscriberEvent<II, OO> discardAllInput(Connection<II, OO> connection) {
        return new ConnectionInputSubscriberEvent<>(Subscribers.create(new Action1<II>() {
            @Override
            public void call(II msg) {
                ReferenceCountUtil.release(msg);
            }
        }), connection);
    }
}
