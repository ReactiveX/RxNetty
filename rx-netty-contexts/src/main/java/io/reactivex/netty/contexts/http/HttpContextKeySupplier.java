package io.reactivex.netty.contexts.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.netty.contexts.ContextKeySupplier;

/**
 * An implementation of {@link ContextKeySupplier} that reads context keys from HTTP headers.
 *
 * @author Nitesh Kant
 */
public class HttpContextKeySupplier implements ContextKeySupplier {

    private final HttpHeaders headers;

    public HttpContextKeySupplier(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public String getContextValue(String key) {
        return headers.get(key);
    }
}
