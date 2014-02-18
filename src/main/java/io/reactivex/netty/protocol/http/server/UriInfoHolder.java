package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class UriInfoHolder {

    private final String uri;
    private final String queryString;
    private final QueryStringDecoder decoder;

    public UriInfoHolder(String uri) {
        this.uri = uri;
        int indexOfStartOfQP = uri.indexOf('?');
        if (-1 != indexOfStartOfQP && uri.length() >= indexOfStartOfQP) {
            queryString = uri.substring(indexOfStartOfQP + 1);
        } else {
            queryString = "";
        }
        decoder = new QueryStringDecoder(uri);
    }

    public String getRawUriString() {
        return uri;
    }

    public synchronized String getPath() {
        return decoder.path();
    }

    public String getQueryString() {
        return queryString;
    }

    public synchronized Map<String, List<String>> getQueryParameters() {
        return decoder.parameters();
    }
}
