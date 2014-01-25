package io.reactivex.netty.protocol.http;

import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.client.RxClient;
import rx.Observable;

public interface HttpClient<I extends HttpRequest, O> extends RxClient<I, O>{

    Observable<ObservableHttpResponse<O>> submit(I request);

    Observable<ObservableHttpResponse<O>> submit(I request, RequestConfig config);

    /**
     * A configuration to be used for a request made through this client.
     */
    class RequestConfig {

        public static final RequestConfig DEFAULT_CONFIG = new RequestConfig();

        private String userAgent = "RxNetty Client";

        private RequestConfig() {
            // Only the builder can create this instance, so that we can change the constructor signature at will.
        }

        public String getUserAgent() {
            return userAgent;
        }

        public static class Builder {

            private final RequestConfig config;

            public Builder(RequestConfig defaultConfig) {
                config = null == defaultConfig ? new RequestConfig() : defaultConfig;
            }

            public Builder userAgent(String userAgent) {
                config.userAgent = userAgent;
                return this;
            }

            public RequestConfig build() {
                return config;
            }
        }
    }
}
