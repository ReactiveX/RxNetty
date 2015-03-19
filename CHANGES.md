# RxNetty Releases #

### Version 0.4.7 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.4.7+is%3Aclosed)

* [Pull 341] (https://github.com/ReactiveX/RxNetty/pull/341) UnicastContentSubject unsubscribes eagerly.
* [Pull 334] (https://github.com/ReactiveX/RxNetty/pull/334) Unsubscribe from auto release timeout if a subscriber comes along.

Artifacts: [Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxnetty%7C0.4.7%7C)

### Version 0.4.6 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.4.6+is%3Aclosed)

* [Pull 327] (https://github.com/ReactiveX/RxNetty/pull/327) HttpServerRequest.getPath should only return path.
* [Pull 328] (https://github.com/ReactiveX/RxNetty/pull/328) Race condition in ClientRequestResponseConverter.
* [Pull 329] (https://github.com/ReactiveX/RxNetty/pull/329) Removing client name uniqueness.
* [Pull 330] (https://github.com/ReactiveX/RxNetty/pull/330) Updating netty to 4.0.25.Final.

Artifacts: [Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxnetty%7C0.4.6%7C)

### Version 0.4.5 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.4.5+is%3Aclosed)

* [Pull 315] (https://github.com/ReactiveX/RxNetty/pull/315) Fixes connection leak for SSL failures.
* [Pull 313] (https://github.com/ReactiveX/RxNetty/pull/313) Only trigger the timeoutScheduler when not yet subscribed.
* [Pull 310] (https://github.com/ReactiveX/RxNetty/pull/310) Fixed race in UnicastContentSubject and discard of connection.
* [Pull 302] (https://github.com/ReactiveX/RxNetty/pull/302) Added module rxnetty-spectator. This will replace rxnetty-servo.

Artifacts: [Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxnetty%7C0.4.5%7C)

### Version 0.4.1 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.4.1+is%3Aclosed)

* [Issue 277] (https://github.com/ReactiveX/RxNetty/issue/277) StackOverflow while draining`UnicastContentSubject`
* [Issue 287] (https://github.com/ReactiveX/RxNetty/issue/287) Flush HTTP response on completion of `RequestHandler.handle()`
* [Issue 288] (https://github.com/ReactiveX/RxNetty/issue/288) Upgrade to rx-java 1.0.1


Artifacts: [Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxnetty%7C0.4.1%7C)

### Version 0.4.0 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.4.0+is%3Aclosed)

* [Pull 275] (https://github.com/ReactiveX/RxNetty/pull/275) Changing package and artifact org from netflix.rxjava -> io.reactivex.  

Artifacts: [Maven Central](http://search.maven.org/#artifactdetails%7Cio.reactivex%7Crxnetty%7C0.4.0%7C)

### Version 0.3.18 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.18+is%3Aclosed)

* [Issue 257] (https://github.com/Netflix/RxNetty/issues/257) Support relative URIs for redirect.  
* [Issue 269] (https://github.com/Netflix/RxNetty/issues/269) `ServerSentEventDecoder` stops decoding after receiving an illegal field.  
* [Issue 270] (https://github.com/Netflix/RxNetty/issues/270) HttpClient should only do relative redirects.  
* [Issue 271] (https://github.com/Netflix/RxNetty/issues/271) URI fragment must be inherited by the redirect location.  
* [Issue 272] (https://github.com/Netflix/RxNetty/issues/272) Guaranteeing sequential notifications from operators/Subjects.  

##### Incompatible changes

* [Issue 270] (https://github.com/Netflix/RxNetty/issues/270) HttpClient should only do relative redirects.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.18%22)

### Version 0.3.17 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.17+is%3Aclosed)

* [Issue 267] (https://github.com/Netflix/RxNetty/issues/267) Change ServerSentEvent to store data as `ByteBuf`.  
* [Pull 265] (https://github.com/ReactiveX/RxNetty/pull/265) Release `LastHttpContent` when it is not readable.
* [Issue 263] (https://github.com/Netflix/RxNetty/issues/263) Rename eventloops.
* [Issue 209] (https://github.com/ReactiveX/RxNetty/issues/209) Deprecate all SSE classes in io.reactivex.netty.protocol.text.sse.
* [Issue 205] (https://github.com/ReactiveX/RxNetty/issues/205) Move SSE related class to the http package.
* [Issue 220] (https://github.com/ReactiveX/RxNetty/issues/220) Confusing ServerSentEvent API causes emitting empty "event" fields.
* [Issue 222] (https://github.com/ReactiveX/RxNetty/issues/222) SSE API is error prone, and lacks flexibility for common usage patterns.
* [Issue 30] (https://github.com/ReactiveX/RxNetty/issues/30) SSE codec re-implementation.


##### Deprecations

* [Issue 209] (https://github.com/ReactiveX/RxNetty/issues/209) Deprecate all SSE classes in io.reactivex.netty.protocol.text.sse.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.17%22)

### Version 0.3.16 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.16+is%3Aclosed)

* [Issue 258] (https://github.com/Netflix/RxNetty/issues/258) Include port in the HTTP HOST header.
* [Issue 259] (https://github.com/Netflix/RxNetty/issues/259) Support native netty protocol as a runtime choice.
* [Issue 260] (https://github.com/Netflix/RxNetty/issues/260) Convert `RxEventLoopProvider` and `MetricEventsListenerFactory` to abstract classes.
* [Issue 261] (https://github.com/Netflix/RxNetty/issues/261) `ServerSentEventEncoder` is sub-optimal in using StringBuilder

##### Incompatible changes

* [Issue 260] (https://github.com/Netflix/RxNetty/issues/260) Convert `RxEventLoopProvider` and `MetricEventsListenerFactory` to abstract classes.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.16%22)

### Version 0.3.15 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.15+is%3Aclosed)

* [Issue 208] (https://github.com/Netflix/RxNetty/issues/208) HTTPClient should discard a connection when response isn't completely consumed.
* [Issue 229] (https://github.com/Netflix/RxNetty/issues/229) Remove deprecated method `DefaultChannelWriter.getChannelHandlerContext()`.
* [Issue 237] (https://github.com/Netflix/RxNetty/issues/237) Empty Client Request Buffers Are Not Released.
* [Issue 243] (https://github.com/Netflix/RxNetty/issues/243) HttpServerListener requestReadTimes metric value is incorrect.
* [Issue 245] (https://github.com/Netflix/RxNetty/issues/245) Race condition between write and flush in `HttpClient.submit()`.
* [Issue 248] (https://github.com/Netflix/RxNetty/issues/248) Connection close cancels pending writes.
* [Issue 249] (https://github.com/Netflix/RxNetty/issues/249) `BytesInspector` does not record events for `ByteBufHolder` and `FileRegion`.
* [Issue 250] (https://github.com/Netflix/RxNetty/issues/250) Fix connection closing and content stream behavior.
* [Issue 251] (https://github.com/Netflix/RxNetty/issues/251) Metric events for Request content source error and request write failure.
* [Issue 252] (https://github.com/Netflix/RxNetty/issues/252) Upgrade RxJava to 1.0.0-RC7.

##### Deprecation removals

* [Issue 229] (https://github.com/Netflix/RxNetty/issues/229) Remove deprecated method `DefaultChannelWriter.getChannelHandlerContext()`.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.15%22)

### Version 0.3.14 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.14+is%3Aclosed)

* [Issue 233] (https://github.com/Netflix/RxNetty/issues/233) Using DefaultFactories.TRUST_ALL causes SelfSignedSSLEngineFactory to Error on Certain Systems.
* [Pull 219] (https://github.com/ReactiveX/RxNetty/pull/219) Http RequestHandler to serve files.
* [Issue 235] (https://github.com/Netflix/RxNetty/issues/235) HttpServer with non-aggregated request is broken.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.14%22)

### Version 0.3.13 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.13+is%3Aclosed)

* [Issue 214] (https://github.com/Netflix/RxNetty/issues/214) HttpServerRequest should provide a way to access Netty channel.
* [Issue 216] (https://github.com/Netflix/RxNetty/issues/216) Remove deprecated HttpClientResponse and HttpServerRequest constructors.
* [Issue 218] (https://github.com/Netflix/RxNetty/issues/218) Migrate Branch Master -> 0.x.
* [Issue 223] (https://github.com/Netflix/RxNetty/issues/223) Client connections receiving SSE responses should never be pooled & reused.
* [Issue 225] (https://github.com/Netflix/RxNetty/issues/225) Unsubscribing from HttpClient.submit() should not close connection.
* [Issue 226] (https://github.com/Netflix/RxNetty/issues/226) Response should be flushed on `RequestHandler`'s failure.
* [Issue 228] (https://github.com/Netflix/RxNetty/issues/228) `DefaultChannelWriter` should return `Channel` instead of `ChannelHandlerContext`.
* [Issue 230] (https://github.com/Netflix/RxNetty/issues/230) FlatResponseOperator does not emit any item if there is no content.

##### Deprecations

* [Issue 228] (https://github.com/Netflix/RxNetty/issues/228) `DefaultChannelWriter.getChannelHandlerContext()` is deprecated.

##### Deprecation removals

* [Issue 216] (https://github.com/Netflix/RxNetty/issues/216) Remove deprecated HttpClientResponse and HttpServerRequest constructors.


Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.13%22)

### Version 0.3.12 ###

[Milestone](https://github.com/ReactiveX/RxNetty/issues?q=milestone%3A0.3.12+is%3Aclosed)

* [Issue 118] (https://github.com/Netflix/RxNetty/issues/118) Javadoc errors when compiling with Java 8.
* [Pull 196] (https://github.com/Netflix/RxNetty/pull/196) Websocket client and server implementation.
* [Pull 204] (https://github.com/Netflix/RxNetty/pull/204) Add a generic Handler interface
* [Issue 206] (https://github.com/Netflix/RxNetty/issues/206) HttpClientResponse.getContent() will loose data if not eagerly subscribed.
* [Issue 199] (https://github.com/Netflix/RxNetty/issues/199) Invalid metric event used in DefaultChannelWriter.

##### Change in behavior

As part of  [Issue 206] (https://github.com/Netflix/RxNetty/issues/206) the following change in behavior is done in this release:

##### Old Behavior
- Before this fix, the the `Observable` returned from `HttpClient.submit()` used to complete after the content of the response completed.
- The content could be lost (this issue) if the content was subscribed out of the `onNext` call of `HttpClientResponse`

##### New Behavior
- After this fix, the `Observable` returned from `HttpClient.submit()` completes immediately after one callback of `HttpClientResponse`. 
- The content can be subscribed out of the `onNext` call of `HttpClientResponse` till the content timeout as described in #206.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.12%22)

### Version 0.3.11 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=9&state=closed)

* [Issue 195] (https://github.com/Netflix/RxNetty/issues/195) RxClientImpl.shutdown() should not shutdown the eventloop.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.11%22)

### Version 0.3.10 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=8&state=closed)

* [Issue 183] (https://github.com/Netflix/RxNetty/issues/183) IdleConnectionsTimeoutMillis was not used by PooledConnectionFactory.
* [Pull 186] (https://github.com/Netflix/RxNetty/pull/186) Added a perf optimized helloworld example.
* [Issue 187] (https://github.com/Netflix/RxNetty/issues/187) Simplifying Aggregated Client Response usage.
* [Pull 188] (https://github.com/Netflix/RxNetty/pull/188) Adding an option to use a threadpool for Request/Connection processing. 

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.10%22)

### Version 0.3.9 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=7&state=closed)

* [Pull 179] (https://github.com/Netflix/RxNetty/issues/179) Monitors were not getting registered with servo.
* [Pull 180] (https://github.com/Netflix/RxNetty/issues/180) HTTP client/server metrics are not getting published to servo.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.9%22)

### Version 0.3.8 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=5&state=closed)

* [Issue 150] (https://github.com/Netflix/RxNetty/issues/150) Removed deprecated PoolStats.
* [Issue 169] (https://github.com/Netflix/RxNetty/issues/169) Removed ContentSource in favor of Observable for HttpClientRequest
* [Issue 172] (https://github.com/Netflix/RxNetty/issues/172) Missing connection pool events from client built by RxContexts constructs
* [Issue 175] (https://github.com/Netflix/RxNetty/issues/175) Optionally disable System time calls for metrics
* [Issue 177] (https://github.com/Netflix/RxNetty/issues/177) For HTTP server use channelReadComplete() to flush writes

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.8%22)

### Version 0.3.7 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=4&state=closed)

* [Issue 98] (https://github.com/Netflix/RxNetty/issues/98) Added pluggable metrics infrastructure with rxnetty-servo implementation.
* [Issue 106] (https://github.com/Netflix/RxNetty/issues/106) Added TLS support for TCP & HTTP (HTTPS)
* [Issue 115] (https://github.com/Netflix/RxNetty/issues/115) ByteBuf leak fixed for both HTTP client and server.
* [Issue 141] (https://github.com/Netflix/RxNetty/issues/141) ServerSentEventEncoder modified to match the specifications and the decoder.
* [Issue 158] (https://github.com/Netflix/RxNetty/issues/158) HttpClientResponse and HttpServerRequest modified to take Subject instead of PublishSubject in the constructors.
* [Issue 160] (https://github.com/Netflix/RxNetty/issues/160) ServerRequestResponseConverter was using the same content subject for all requests on a channel.
* [Issue 164] (https://github.com/Netflix/RxNetty/issues/164) Removed flatmap() usage from HttpConnectionHandler for performance reasons.
* [Issue 166] (https://github.com/Netflix/RxNetty/issues/166) RxServer modified to optionally use a separate acceptor eventloop group.
* [Issue 167] (https://github.com/Netflix/RxNetty/issues/167) Not sending "Connection: keep-alive" header for HTTP 1.1 requests.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.7%22)

### Version 0.3.6 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=2&state=closed)

* [Issue 111] (https://github.com/Netflix/RxNetty/issues/111) Unsubscribing an SSE stream does not close the underlying connection
* [Issue 126] (https://github.com/Netflix/RxNetty/issues/126) HttpServer not compliant to HTTP 1.0 requests
* [Issue 127] (https://github.com/Netflix/RxNetty/issues/127) Revamp the examples.
* [Issue 129] (https://github.com/Netflix/RxNetty/issues/129) HTTP Redirect fails when there is a connection pool
* [Issue 130] (https://github.com/Netflix/RxNetty/issues/130) HTTP keep-alive connections not reusable with chunked transfer encoding
* [Issue 131] (https://github.com/Netflix/RxNetty/issues/131) HTTP redirect loop & max redirect detection is broken
* [Issue 134] (https://github.com/Netflix/RxNetty/issues/134) Provide default http header name for request ID in request context
* [Pull 135] (https://github.com/Netflix/RxNetty/issues/135) Added convenience method to add a single object with ContentTransformer to the HttpClientRequest
* [Issue 139] (https://github.com/Netflix/RxNetty/issues/139) HttpServerResponse.close() should be idempotent
* [Issue 143] (https://github.com/Netflix/RxNetty/issues/143) Write FullHttpResponse when possible
* [Issue 145] (https://github.com/Netflix/RxNetty/issues/145) ObservableConnection.cleanupConnection() does a blocking call.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.6%22)

### Version 0.3.5 ###

[Milestone](https://github.com/Netflix/RxNetty/issues?milestone=1&page=1&state=closed)

* [Issue 55] (https://github.com/Netflix/RxNetty/issues/55) Add wire debugging functionality
* [Issue 88] (https://github.com/Netflix/RxNetty/issues/88) Move host and port to RxClient method signature
* [Issue 90] (https://github.com/Netflix/RxNetty/issues/90) Server onError Handling
* [Issue 101] (https://github.com/Netflix/RxNetty/issues/101) Infrastructure Contexts
* [Issue 105] (https://github.com/Netflix/RxNetty/issues/105) AbstractClientBuilder.SHARED_IDLE_CLEANUP_SCHEDULER prevents application from closing.
* [Pull 108] (https://github.com/Netflix/RxNetty/issues/108) UDP support.
* [Issue 113] (https://github.com/Netflix/RxNetty/issues/113) Implement Redirect as an Rx Operator.
* [Issue 114] (https://github.com/Netflix/RxNetty/issues/114) Remove numerus dependency from rxnetty-core.
* [Pull 116] (https://github.com/Netflix/RxNetty/issues/116) Added toString() to ServerInfo for better inspection.
* [Issue 117] (https://github.com/Netflix/RxNetty/issues/117) RxClientImpl mutates netty's bootstrap per connection
* [Issue 119] (https://github.com/Netflix/RxNetty/issues/119) "Transfer-encoding: chunked" not set for HTTP POST request.

Artifacts: [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22%20AND%20v%3A%220.3.5%22)

### Version 0.3.4 ###

Skipped release due to manual tag creation.
