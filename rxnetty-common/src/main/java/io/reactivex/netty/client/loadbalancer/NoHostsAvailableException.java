/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.client.loadbalancer;

import io.netty.util.internal.EmptyArrays;

/**
 * Exception raised when there are no eligible hosts available to a load balancer.
 */
public class NoHostsAvailableException extends RuntimeException {

    private static final long serialVersionUID = 7993688893506534768L;

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    public static final NoHostsAvailableException EMPTY_INSTANCE = new NoHostsAvailableException();

    static {
        EMPTY_INSTANCE.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    }

    public NoHostsAvailableException() {
    }

    public NoHostsAvailableException(String message) {
        super(message);
    }

    public NoHostsAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoHostsAvailableException(Throwable cause) {
        super(cause);
    }

    public NoHostsAvailableException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
