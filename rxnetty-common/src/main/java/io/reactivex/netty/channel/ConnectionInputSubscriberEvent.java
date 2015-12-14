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

import io.netty.util.ReferenceCountUtil;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.Subscribers;

/**
 * An event to communicate the subscriber of the associated connection input stream created by
 * {@link AbstractConnectionToChannelBridge}.
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

    private final Subscriber<? super R> subscriber;

    public ConnectionInputSubscriberEvent(Subscriber<? super R> subscriber) {
        if (null == subscriber) {
            throw new NullPointerException("Subscriber can not be null");
        }
        this.subscriber = subscriber;
    }

    public Subscriber<? super R> getSubscriber() {
        return subscriber;
    }

    public static <II, OO> ConnectionInputSubscriberEvent<II, OO> discardAllInput() {
        return new ConnectionInputSubscriberEvent<>(Subscribers.create(new Action1<II>() {
            @Override
            public void call(II msg) {
                ReferenceCountUtil.release(msg);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                // Empty as we are discarding input anyways.
            }
        }));
    }
}
