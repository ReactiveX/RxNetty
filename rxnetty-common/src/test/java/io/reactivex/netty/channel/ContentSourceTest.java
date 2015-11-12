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

import io.netty.buffer.ByteBuf;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

public class ContentSourceTest {

    @Rule
    public final ContentSourceRule sourceRule = new ContentSourceRule();

    @Test(timeout = 60000)
    public void testNoAutoRelease() throws Exception {
        ContentSource<ByteBuf> source = new ContentSource<>(sourceRule.getChannel(), sourceRule);
        sourceRule.subscribe(source, 1);
    }

    @Test(timeout = 60000)
    public void testAutoRelease() throws Exception {
        ContentSource<ByteBuf> source = new ContentSource<>(sourceRule.getChannel(), sourceRule);
        sourceRule.subscribe(source.autoRelease(), 0);
    }

    @Test(timeout = 60000)
    public void testReplayable() throws Exception {
        DisposableContentSource<ByteBuf> disposable = new ContentSource<>(sourceRule.getChannel(), sourceRule).replayable();
        sourceRule.subscribe(disposable.autoRelease(), 1);
        sourceRule.subscribe(disposable.autoRelease(), 1);

        assertThat("Unexpected ref count before dispose.", sourceRule.getData().refCnt(), is(1));

        disposable.dispose();

        assertThat("Unexpected ref count after dispose.", sourceRule.getData().refCnt(), is(0));
    }

    @Test(timeout = 60000)
    public void testSubscribePostDispose() throws Exception {
        DisposableContentSource<ByteBuf> disposable = new ContentSource<>(sourceRule.getChannel(), sourceRule).replayable();
        sourceRule.subscribe(disposable.autoRelease(), 1);
        disposable.dispose();

        TestSubscriber<ByteBuf> subscriber = new TestSubscriber<>();
        disposable.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertError(IllegalStateException.class);
    }
}