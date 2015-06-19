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

package io.reactivex.netty;

import io.reactivex.netty.channel.RxEventLoopProvider;
import io.reactivex.netty.channel.SingleNioLoopProvider;

public final class RxNetty {

    private static volatile RxEventLoopProvider rxEventLoopProvider = new SingleNioLoopProvider(Runtime.getRuntime().availableProcessors());

    private static volatile boolean usingNativeTransport;
    private static volatile boolean disableEventPublishing;

    private RxNetty() {
    }

    /**
     * An implementation of {@link RxEventLoopProvider} to be used by all clients and servers created after this call.
     *
     * @param provider New provider to use.
     *
     * @return Existing provider.
     */
    public static RxEventLoopProvider useEventLoopProvider(RxEventLoopProvider provider) {
        RxEventLoopProvider oldProvider = rxEventLoopProvider;
        rxEventLoopProvider = provider;
        return oldProvider;
    }

    public static RxEventLoopProvider getRxEventLoopProvider() {
        return rxEventLoopProvider;
    }

    /**
     * A global flag to start using netty's <a href="https://github.com/netty/netty/wiki/Native-transports">native protocol</a>
     * if applicable for a client or server.
     *
     * <b>This does not evaluate whether the native transport is available for the OS or not.</b>
     *
     * So, this method should be called conditionally when the caller is sure that the OS supports the native protocol.
     *
     * Alternatively, this can be done selectively per client and server instance.
     */
    public static void useNativeTransportIfApplicable() {
        usingNativeTransport = true;
    }

    /**
     * A global flag to disable the effects of calling {@link #useNativeTransportIfApplicable()}
     */
    public static void disableNativeTransport() {
        usingNativeTransport = false;
    }

    /**
     * Enables publishing of events for RxNetty.
     */
    public static void enableEventPublishing() {
        disableEventPublishing = false;
    }

    /**
     * Disables publishing of events for RxNetty.
     */
    public static void disableEventPublishing() {
        disableEventPublishing = true;
    }

    /**
     * Returns {@code true} if event publishing is disabled.
     *
     * @return {@code true} if event publishing is disabled.
     */
    public static boolean isEventPublishingDisabled() {
        return disableEventPublishing;
    }

    public static boolean isUsingNativeTransport() {
        return usingNativeTransport;
    }
}
