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

import io.netty.channel.EventLoopGroup;
import rx.Scheduler;
import rx.annotations.Beta;
import rx.plugins.RxJavaSchedulersHook;
import rx.schedulers.Schedulers;

/**
 * A scheduler hook for RxJava, to override the computation scheduler, as retrieved via {@link Schedulers#computation()},
 * with a scheduler to use netty's {@link EventLoopGroup}. The computation scheduler implementation is as provided as an
 * {@link RxJavaEventloopScheduler} instance. <p>
 *
 * This is to be used as <p>
 {@code
       RxJavaPlugins.getInstance().registerSchedulersHook(hook);
 }
 at startup.
 */
@Beta
public class RxJavaNettyBasedSchedulersHook extends RxJavaSchedulersHook {

    private final RxJavaEventloopScheduler computationScheduler;

    public RxJavaNettyBasedSchedulersHook(RxJavaEventloopScheduler computationScheduler) {
        this.computationScheduler = computationScheduler;
    }

    @Override
    public Scheduler getComputationScheduler() {
        return computationScheduler;
    }
}
