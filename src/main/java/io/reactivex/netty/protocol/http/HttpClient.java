package io.reactivex.netty.protocol.http;

import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.client.RxClient;
import rx.Observable;

public interface HttpClient<I extends HttpRequest, O> extends RxClient<I, O>{

    Observable<ObservableHttpResponse<O>> submit(I request);

    Observable<ObservableHttpResponse<O>> submit(I request, ClientConfig config);

    /**
     * A configuration to be used for this client.
     */
    class HttpClientConfig extends ClientConfig {

        public static final HttpClientConfig DEFAULT_CONFIG = new HttpClientConfig();

        private String userAgent = "RxNetty Client";

        private HttpClientConfig() {
            // Only the builder can create this instance, so that we can change the constructor signature at will.
        }

        public String getUserAgent() {
            return userAgent;
        }

        public static class Builder extends AbstractBuilder<Builder, HttpClientConfig> {

            public Builder(HttpClientConfig defaultConfig) {
                super(null == defaultConfig ? new HttpClientConfig() : defaultConfig);
            }

            public Builder userAgent(String userAgent) {
                config.userAgent = userAgent;
                return returnBuilder();
            }
        }
    }
}
