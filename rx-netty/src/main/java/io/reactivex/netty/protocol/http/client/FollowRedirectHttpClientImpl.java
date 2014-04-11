package io.reactivex.netty.protocol.http.client;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.functions.Func1;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.pool.ChannelPool;
import io.reactivex.netty.pipeline.PipelineConfigurator;

class FollowRedirectHttpClientImpl<I,O> extends HttpClientImpl<I, O> {

    static final int MAX_HOPS = 5;
    
    public static class RedirectException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public RedirectException(String arg0) {
            super(arg0);
        }

        public RedirectException(String string, Exception e) {
            super(string, e);
        }
    }
    
    private final ConcurrentLinkedQueue<String> visited;
    private volatile String requestedLocation;
    
    protected FollowRedirectHttpClientImpl(
            io.reactivex.netty.client.RxClient.ServerInfo serverInfo,
            Bootstrap clientBootstrap,
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
            io.reactivex.netty.client.RxClient.ClientConfig clientConfig,
            ChannelPool pool) {
        this(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, pool, new ConcurrentLinkedQueue<String>(), null);
    }
    
    protected FollowRedirectHttpClientImpl(
            io.reactivex.netty.client.RxClient.ServerInfo serverInfo,
            Bootstrap clientBootstrap,
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
            io.reactivex.netty.client.RxClient.ClientConfig clientConfig,
            ChannelPool pool, ConcurrentLinkedQueue<String> visited, String location) {
        super(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, pool);
        this.visited = visited;  
        this.requestedLocation = location;
    }
    
    protected FollowRedirectHttpClientImpl(
            io.reactivex.netty.client.RxClient.ServerInfo serverInfo,
            Bootstrap clientBootstrap,
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
            io.reactivex.netty.client.RxClient.ClientConfig clientConfig, ConcurrentLinkedQueue<String> visited, String location) {
        super(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig);
        this.visited = visited;
        this.requestedLocation = location;
    }

    @Override
    protected Observable<HttpClientResponse<O>> submit(
            HttpClientRequest<I> request,
            Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable,
            RxClient.ClientConfig config) {
        final HttpClientRequest<I> _request;
        if (!(request instanceof RepeatableContentHttpRequest)) {
            // need to make sure content source 
            // is repeatable when we resubmit the request to the redirected host
            _request = new RepeatableContentHttpRequest<I>(request);
        } else {
            _request = request;
        }
        Observable<HttpClientResponse<O>> originalResponse = super.submitWithoutRedirect(_request, connectionObservable, config);
        return checkForRedirect(_request, originalResponse, config);
    }
    
    private static String getHttpURI(ServerInfo serverInfo, HttpClientRequest<?> request) {
        String uri = request.getUri();
        if (uri.startsWith("http:")) {
            // absolute URI
            return uri;
        }
        StringBuilder sb = new StringBuilder("http://");
        sb.append(serverInfo.getHost())
          .append(":")
          .append(serverInfo.getPort());
        if (!uri.startsWith("/")) {
            sb.append("/");
        }
        sb.append(uri);
        return sb.toString();
    }
    
    private static String getPathQueryFragment(URI uri) {
        StringBuilder sb = new StringBuilder();
        if (uri.getRawPath() != null) {
            sb.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            sb.append("?").append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            sb.append("#").append(uri.getRawFragment());
        }
        return sb.toString();
    }
    
    private Observable<HttpClientResponse<O>> checkForRedirect(final HttpClientRequest<I> request, Observable<HttpClientResponse<O>> originalResponse, final RxClient.ClientConfig config) {
        return originalResponse.flatMap(new Func1<HttpClientResponse<O>, Observable<HttpClientResponse<O>>>() {
            @Override
            public Observable<HttpClientResponse<O>> call(
                    HttpClientResponse<O> responseToCheckRedirect) {
                String visitedLocation = requestedLocation;
                if (requestedLocation == null) {
                    visitedLocation = getHttpURI(serverInfo, request);
                }
                visited.add(visitedLocation);
                int statusCode = responseToCheckRedirect.getStatus().code();
                switch (statusCode) {
                case 301:
                case 302:
                case 303:
                case 307:
                case 308:
                    String location = responseToCheckRedirect.getHeaders().get(HttpHeaders.Names.LOCATION);
                    if (location == null) {
                        return Observable.error(new Exception("Location header is not set in the redirect response"));
                    } 
                    if (visited.contains(location)) {
                        // this forms a loop
                        return Observable.error(new RedirectException(String.format("A loop is formed while following redirects: %s", visited)));
                    } else if (visited.size() == MAX_HOPS) {
                        // we have reached the limit of locations to follow redirect
                        return Observable.error(new RedirectException(String.format("Maximum redirections reached: %s", visited)));
                    }
                    URI uri;
                    try {
                        uri = new URI(location);
                    } catch (Exception e) {
                        return Observable.error(new RedirectException("Location is not a valid URI", e));
                    }
                    if (!uri.isAbsolute()) {
                        // Redirect URI must be absolute
                        return Observable.error(new RedirectException(String.format("Location header %s is not absolute", location)));
                    }
                    String host = uri.getHost();
                    if (host == null) {
                        return Observable.error(new RedirectException(String.format("Location header %s missing host name", location)));
                    }
                    int port = uri.getPort();
                    if (port < 0) {
                        port = 80;
                    }
                    HttpClientRequest<I> newRequest = createRedirectRequest(request, getPathQueryFragment(uri), statusCode);
                    newRequest.getHeaders().set(HttpHeaders.Names.HOST, host);
                    if (host.equals(serverInfo.getHost()) && port == serverInfo.getPort()) {
                        // same server, no need to create new client 
                        requestedLocation = location;
                        return submit(newRequest, config);
                    } else {
                        FollowRedirectHttpClientImpl<I, O> redirectClient = 
                                new FollowRedirectHttpClientImpl<I, O>(new ServerInfo(host, port), clientBootstrap, originalPipelineConfigurator, config, visited, location);
                        return redirectClient.submit(newRequest, config);
                    }
                default:
                    break;
                }
                return Observable.from(responseToCheckRedirect);
            }
        });
    }
    
    private static <I> HttpClientRequest<I> createRedirectRequest(HttpClientRequest<I> original, String newURI, int statusCode) {
        HttpRequest nettyRequest = original.getNettyRequest();
        nettyRequest.setUri(newURI);
        if (statusCode == 303) {
            // according to HTTP spec, 303 mandates the change of request type to GET
            nettyRequest.setMethod(HttpMethod.GET);
        }
        HttpClientRequest<I> newRequest = new HttpClientRequest<I>(nettyRequest);
        if (statusCode != 303) {
            // if status code is 303, we can just leave the content factory to be null
            newRequest.contentFactory = original.contentFactory;
            newRequest.rawContentFactory = original.rawContentFactory;
        }
        return newRequest;
    }
}
