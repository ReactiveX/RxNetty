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
package io.reactivex.netty.protocol.http.sse;

import io.netty.handler.codec.DecoderException;

/**
 * Exception when the field name of a Server Sent Event is more than
 *
 * @author Nitesh Kant
 */
public class TooLongFieldNameException  extends DecoderException {

    private static final long serialVersionUID = 5592673637644375829L;

    public TooLongFieldNameException() {
    }

    public TooLongFieldNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooLongFieldNameException(String message) {
        super(message);
    }

    public TooLongFieldNameException(Throwable cause) {
        super(cause);
    }
}
