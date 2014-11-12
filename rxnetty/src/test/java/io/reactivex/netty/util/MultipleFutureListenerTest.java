/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.util;

import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;
import rx.functions.Action0;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class MultipleFutureListenerTest {

    @Test
    public void testCompletionWhenNoFuture() throws Exception {
        NoOpChannelHandlerContext context = new NoOpChannelHandlerContext();
        MultipleFutureListener listener = new MultipleFutureListener(context.newPromise());
        final CountDownLatch completionLatch = new CountDownLatch(1);
        listener.asObservable().doOnTerminate(new Action0() {
            @Override
            public void call() {
                completionLatch.countDown();
            }
        }).subscribe();
        completionLatch.await(1, TimeUnit.MINUTES);
    }
}
