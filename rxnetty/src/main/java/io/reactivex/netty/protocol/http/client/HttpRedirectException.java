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
package io.reactivex.netty.protocol.http.client;

/**
 * An exception signifying a failed HTTP redirects. Every exception has an encapsulated {@link Reason} retrievable via
 * {@link #getReason()}
 *
 * @author Nitesh Kant
 */
public class HttpRedirectException extends RuntimeException {

    private static final long serialVersionUID = 612647744832660373L;
    private final Reason reason;

    public enum Reason {
        RedirectLoop,
        TooManyRedirects,
        InvalidRedirect
    }

    public HttpRedirectException(Reason reason) {
        this.reason = reason;
    }

    public HttpRedirectException(Reason reason, Throwable cause) {
        super(getMsgWithReason(reason), cause);
        this.reason = reason;
    }

    public HttpRedirectException(Reason reason, String message) {
        super(getMsgWithReason(reason, message));
        this.reason = reason;
    }

    public HttpRedirectException(Reason reason, String message, Throwable cause) {
        super(getMsgWithReason(reason, message), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    private static String getMsgWithReason(Reason reason) {
        return "Redirect failed. Reason: " + reason;
    }

    private static String getMsgWithReason(Reason reason, String message) {
        return getMsgWithReason(reason) + ". Error: " + message;
    }
}
