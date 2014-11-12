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
package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Encapsulate an exception with a specific HTTP response code
 * so a proper HTTP error response may be generated.
 * 
 * @author elandau
 *
 */
public class HttpError extends Exception {
    private final HttpResponseStatus status;
    
    public HttpError(HttpResponseStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpError(HttpResponseStatus status, String message, Throwable t) {
        super(message, t);
        this.status = status;
    }
    public HttpError(HttpResponseStatus status, Throwable t) {
        super(t);
        this.status = status;
    }
    public HttpError(HttpResponseStatus status) {
        this.status = status;
    }
    public HttpResponseStatus getStatus() {
        return status;
    }
    
    public String toString() {
        return "" + status.toString() + " : " + super.toString();
    }
}
