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

package io.reactivex.netty.threads;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import rx.Scheduler;
import rx.Subscription;
import rx.annotations.Beta;
import rx.functions.Action0;
import rx.internal.util.SubscriptionList;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

/**
 * A scheduler that uses a provided {@link EventLoopGroup} instance to schedule tasks. This should typically be used as
 * a computation scheduler or any other scheduler that do not schedule blocking tasks. <p>
 */
@Beta
public class RxJavaEventloopScheduler extends Scheduler {

    private final EventLoopGroup eventLoopGroup;

    public RxJavaEventloopScheduler(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public Worker createWorker() {
        final EventLoop eventLoop = eventLoopGroup.next();

        return new Worker() {

            private final SubscriptionList subs = new SubscriptionList();

            @Override
            public Subscription schedule(final Action0 action) {
                if (isUnsubscribed()) {
                    return Subscriptions.unsubscribed();
                }

                /*If already on the eventloop then execute the action, else schedule it on the eventloop*/
                if (eventLoop.inEventLoop()) {
                    action.call();
                    return Subscriptions.empty();
                } else {
                    final Future<?> result = eventLoop.submit(new Runnable() {
                        @Override
                        public void run() {
                            action.call();
                        }
                    });

                    Subscription toReturn = fromFuture(result);
                    subs.add(toReturn);
                    return toReturn;
                }
            }

            @Override
            public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {

                if (delayTime <= 0) {
                    return schedule(action);
                }

                if (isUnsubscribed()) {
                    return Subscriptions.unsubscribed();
                }

                final Future<?> result = eventLoop.schedule(new Runnable() {
                    @Override
                    public void run() {
                        action.call();
                    }
                }, delayTime, unit);

                Subscription toReturn = fromFuture(result);
                subs.add(toReturn);
                return toReturn;
            }

            @Override
            public void unsubscribe() {
                subs.unsubscribe();
            }

            @Override
            public boolean isUnsubscribed() {
                return subs.isUnsubscribed();
            }

            private Subscription fromFuture(final Future<?> result) {
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        result.cancel(false);
                    }
                });
            }
        };
    }

}
