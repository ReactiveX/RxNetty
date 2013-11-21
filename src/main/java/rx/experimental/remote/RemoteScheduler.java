/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.experimental.remote;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

public abstract class RemoteScheduler {

    public <T> RemoteSubscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return null;
    }

    /**
     * Parallelism available to a Scheduler.
     * <p>
     * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
     * 
     * @return the scheduler's available degree of parallelism.
     */
    public int degreeOfParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

}
