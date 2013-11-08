/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.netty.experimental.protocol.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class ObservableHttpClient {
    private EventLoopGroup eventLoopGroup;

    // Chunk size in bytes
    private final int maxChunkSize;

    // Maximum initial line length in characters
    private final int maxInitialLineLength;

    // Maximum header size in bytes
    private final int maxHeaderSize;

    // Whether use compression when sending request
    private final boolean useCompression;

    // Flag on whether the client should follow HTTP redirects
    private final boolean followRedirects;

    // use agent string
    private final String userAgent;

    private final Set<ChannelSetting> channelSettings;

    private final EventExecutor eventExecutor;

    public ObservableHttpClient(
            EventLoopGroup eventLoopGroup,
            int maxChunkSize,
            int maxInitialLineLength,
            int maxHeaderSize,
            boolean useCompression,
            boolean followRedirects,
            String userAgent,
            Set<ChannelSetting> channelOptions,
            EventExecutor eventExecutor) {
        this.eventLoopGroup = eventLoopGroup;
        this.maxChunkSize = maxChunkSize;
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxHeaderSize = maxHeaderSize;
        this.useCompression = useCompression;
        this.followRedirects = followRedirects;
        this.userAgent = userAgent;
        this.eventExecutor = eventExecutor;

        this.channelSettings = new HashSet<ChannelSetting>();
        for (ChannelSetting setting : channelOptions) {
            this.channelSettings.add(setting);
        }
    }

    private <T> ConnectionPromise<T, HttpRequest> makeConnection(Bootstrap bootstrap, UriInfo uriInfo) {
        final ConnectionPromise<T, HttpRequest> connectionPromise = new ConnectionPromise<T, HttpRequest>(eventExecutor);
        bootstrap.connect(uriInfo.getHost(), uriInfo.getPort())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            connectionPromise.onConnect(future.channel());
                        } else {
                            connectionPromise.tryFailure(future.cause());
                        }
                    }
                });

        return connectionPromise;
    }

    public Observable<ObservableHttpResponse<Message>> stream(final ValidatedFullHttpRequest request) {
        return execute(request, HttpProtocolHandlerAdapter.SSE_HANDLER);
    }

    public Observable<ObservableHttpResponse<FullHttpResponse>> request(final ValidatedFullHttpRequest request) {
        return execute(request, HttpProtocolHandlerAdapter.FULL_HTTP_RESPONSE_HANDLER);
    }

    public <T> Observable<ObservableHttpResponse<T>> execute(final ValidatedFullHttpRequest request, final HttpProtocolHandler<T> handler) {
        tryUpdateUserAgent(request);

        final ObservableHttpClient self = this;
        return Observable.create(new Observable.OnSubscribeFunc<ObservableHttpResponse<T>>() {
            @Override
            public Subscription onSubscribe(Observer<? super ObservableHttpResponse<T>> observer) {
                UriInfo uriInfo = request.getUriInfo();
                Bootstrap bootstrap = createBootstrap(handler, observer);
                final ConnectionPromise<T, HttpRequest> connectionPromise = makeConnection(bootstrap, uriInfo);

                RequestCompletionPromise<T, HttpRequest> requestCompletionPromise = new RequestCompletionPromise<T, HttpRequest>(self.eventExecutor, connectionPromise);

                GenericFutureListener<Future<RequestWriter<T, HttpRequest>>> listener = new ConnectionListener<T, HttpRequest>(connectionPromise, request, requestCompletionPromise);
                connectionPromise.addListener(listener);

                return Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        try {
                            connectionPromise.channel().close().sync();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Failed to unsubscribe");
                        }
                    }

                });
            }
        });
    }

    private void tryUpdateUserAgent(ValidatedFullHttpRequest request) {
        if (userAgent != null && request.headers().get(HttpHeaders.Names.USER_AGENT) == null) {
            request.headers().set(HttpHeaders.Names.USER_AGENT, userAgent);
        }
    }

    private static class HttpResponseDecoder<T> extends MessageToMessageDecoder<HttpObject> {
        private final HttpProtocolHandler handler;

        private Observer<? super ObservableHttpResponse<T>> observer;

        public HttpResponseDecoder(HttpProtocolHandler handler, Observer<? super ObservableHttpResponse<T>> observer) {
            this.handler = handler;
            this.observer = observer;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                String header = response.headers().get("Content-Type");
                ContentType contentType = ContentType.fromHeader(header);
                if (contentType.isSSE()) {
                    ChannelPipeline p = ctx.channel().pipeline();
                    p.remove("http-codec");
                    p.replace("http-response-decoder", "http-sse-handler", new ServerSentEventDecoder());
                } else {
                    ctx.channel()
                            .pipeline()
                            .replace("http-response-decoder", "http-aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));

                    out.add(msg);
                }

                final ObservableHttpResponse<T> httpResponse = new ObservableHttpResponse<T>(response, PublishSubject.<T> create());
                observer.onNext(httpResponse);
                ctx.channel().pipeline().addLast("content-handler", new HttpMessageObserver<T>(observer, httpResponse));

                handler.configure(ctx.channel().pipeline());
            }
        }
    }

    private <T> Bootstrap createBootstrap(final HttpProtocolHandler<T> handler, final Observer<? super ObservableHttpResponse<T>> observer) {

        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(this.eventLoopGroup)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("http-codec", new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize))
                                .addLast("http-response-decoder", new HttpResponseDecoder<T>(handler, observer));
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class);

        for (ChannelSetting setting : channelSettings) {
            bootstrap.option(setting.getOption(), setting.getValue());
        }

        return bootstrap;
    }

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        ObservableHttpClient client = new HttpClientBuilder().build(group);

        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get("http://ec2-107-22-122-75.compute-1.amazonaws.com:7001/turbine.stream?cluster=api-prod-c0us.ca");

        request.getUriInfo().getUri().getRawPath();
        Observable<ObservableHttpResponse<Message>> response = client.execute(request, new HttpProtocolHandler<Message>() {

            @Override
            public void configure(ChannelPipeline pipeline) {

            }
        });

        response.flatMap(new Func1<ObservableHttpResponse<Message>, Observable<Message>>() {
            @Override
            public Observable<Message> call(ObservableHttpResponse<Message> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).subscribe(new Action1<Message>() {
            @Override
            public void call(Message message
                    ) {
                System.out.println(message);
            }
        });

        //group.shutdownGracefully();
    }

    /**
     * A class that captures a unique channel option value. This is class is necessary
     * because we can't declare a generic variable in a non-parametric class. That is,
     * we can't simply declare {@code Map<ChannelOption<T>, T> channelSettings} without
     * declaring the parameter {@code T}.
     * 
     * Note this is a special class. It is intended to be used in a collection, and its
     * uniqueness is associated only to the option (so to keep the semantics of {@link io.netty.util.UniqueName},
     * which {@link io.netty.channel.ChannelOption} extends).
     */
    private static final class ChannelSetting<T> {
        private final ChannelOption<T> option;
        private final T value;

        public ChannelSetting(ChannelOption<T> option, T value) {
            this.option = option;
            this.value = value;
        }

        public ChannelOption<T> getOption() {
            return option;
        }

        public T getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ChannelSetting that = (ChannelSetting) o;

            if (option != null ? !option.equals(that.option) : that.option != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return option != null ? option.hashCode() : 0;
        }
    }

    public static HttpClientBuilder newBuilder() {
        return new HttpClientBuilder();
    }

    public static class HttpClientBuilder {
        // Chunk size in bytes
        private int maxChunkSize = 64 * 1024;

        // Maximum initial line length in characters
        private int maxInitialLineLength = 2048;

        // Maximum header size in bytes
        private int maxHeaderSize = 16 * 1024;

        // Whether use compression when sending request
        private boolean useCompression = false;

        // Flag on whether the client should follow HTTP redirects
        private boolean followRedirects = false;

        // Use Agent string sent with each request
        private String userAgent = "RxNetty Client";

        private EventExecutor eventExecutor;

        private Set<ChannelSetting> channelOptions = new HashSet<ChannelSetting>();

        public HttpClientBuilder maxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
            return this;
        }

        public HttpClientBuilder maxInitialLineLength(int maxInitialLineLength) {
            this.maxInitialLineLength = maxInitialLineLength;
            return this;
        }

        public HttpClientBuilder maxHeaderSize(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
            return this;
        }

        public HttpClientBuilder useCompression(boolean useCompression) {
            this.useCompression = useCompression;
            return this;
        }

        public HttpClientBuilder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public HttpClientBuilder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public <T> HttpClientBuilder withChannelOption(ChannelOption<T> option, T value) {
            this.channelOptions.add(new ChannelSetting<T>(option, value));

            return this;
        }

        public HttpClientBuilder withEventExecutor(EventExecutor executor) {
            this.eventExecutor = executor;

            return this;
        }

        public ObservableHttpClient build(EventLoopGroup group) {
            return new ObservableHttpClient(group, maxChunkSize, maxInitialLineLength, maxHeaderSize, useCompression, followRedirects, userAgent, channelOptions, eventExecutor);
        }
    }
}