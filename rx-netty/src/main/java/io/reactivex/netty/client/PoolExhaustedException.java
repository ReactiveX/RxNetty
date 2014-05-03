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
package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class PoolExhaustedException extends Exception {

    private static final long serialVersionUID = -6299997509113653123L;

    public PoolExhaustedException() {
    }

    public PoolExhaustedException(Throwable cause) {
        super(cause);
    }

    public PoolExhaustedException(String message) {
        super(message);
    }

    public PoolExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
