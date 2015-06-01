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
package io.reactivex.netty.client;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.reactivex.netty.channel.client.PreferCurrentEventLoopGroup;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PreferCurrentEventLoopGroupTest {

    @Test(timeout = 60000)
    public void testNextInEventloop() throws Exception {
        final PreferCurrentEventLoopGroup group = new PreferCurrentEventLoopGroup(new NioEventLoopGroup(4));
        Set<EventExecutor> children = group.children();
        for (EventExecutor child : children) {
            Future<Boolean> future = child.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return group.next().inEventLoop();
                }
            });
            assertTrue("Current eventloop was not preferred.", future.get(1, TimeUnit.MINUTES));
        }
    }
}