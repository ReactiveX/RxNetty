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
package io.reactivex.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author Nitesh Kant
*/
@ChannelHandler.Sharable
public class ChannelCloseListener extends ChannelInboundHandlerAdapter {

    private final AtomicBoolean unregisteredCalled = new AtomicBoolean();
    private final Object closeMonitor = new Object();

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        unregisteredCalled.set(true);
        synchronized (closeMonitor) {
            closeMonitor.notifyAll();
        }
    }

    public boolean channelCloseReceived() {
        return unregisteredCalled.get();
    }

    public void reset() {
        unregisteredCalled.set(false);
    }

    public boolean waitForClose(long timeout, TimeUnit timeUnit) throws InterruptedException {
        synchronized (closeMonitor) {
            if (unregisteredCalled.get()) {
                return true;
            }
            closeMonitor.wait(TimeUnit.MILLISECONDS.convert(timeout, timeUnit));
            return unregisteredCalled.get();
        }
    }
}
