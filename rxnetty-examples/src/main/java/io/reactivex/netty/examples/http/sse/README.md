Overview
========

RxNetty supports HTML5 Server-Sent Events (SSE) streams. This feature is provided as a custom pipeline configuration
that includes SSE message encoding and decoding. For more information about SSE look
[here](http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html#event-stream-interpretation).

Server side events are modeled with io.reactivex.netty.protocol.text.sse.ServerSentEvent class.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSseServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSseClient
```

HTTP client
===========

Here is the snippet from [HttpSseClient](HttpSseClient.java):

```java
public List<ServerSentEvent> readServerSideEvents() {
    HttpClient<ByteBuf, ServerSentEvent> client =
            RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

    Iterable<ServerSentEvent> eventIterable = client.submit(HttpClientRequest.createGet("/hello")).
            flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
                @Override
                public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                    printResponseHeader(response);
                    return response.getContent();
                }
            }).take(noOfEvents).toBlocking().toIterable();

    List<ServerSentEvent> events = new ArrayList<ServerSentEvent>();
    for (ServerSentEvent event : eventIterable) {
        System.out.println(event);
        events.add(event);
    }

    return events;
}
```
The HTTP client is created with the predefined SSE pipeline configuration returned by
PipelineConfigurators.<ByteBuf>sseClientConfigurator() method. It injects SSE message
decoder in Netty's request processing pipeline, which converts HTTP reply body byte stream into
ServerSentEvent object stream. Calling HttpClientResponse.getContent method returns an Observable of
ServerSentEvent objects.


HTTP server
===========

Here is the snippet from [HttpSseServer](HttpSseServer.java):

```java
public HttpServer<ByteBuf, ServerSentEvent> createServer() {
    HttpServer<ByteBuf, ServerSentEvent> server = RxNetty.createHttpServer(port,
            new RequestHandler<ByteBuf, ServerSentEvent>() {
                @Override
                public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                               HttpServerResponse<ServerSentEvent> response) {
                    return getIntervalObservable(response);
                }
            }, PipelineConfigurators.<ByteBuf>sseServerConfigurator());
    System.out.println("HTTP Server Sent Events server started...");
    return server;
}

private Observable<Void> getIntervalObservable(final HttpServerResponse<ServerSentEvent> response) {
    return Observable.interval(interval, TimeUnit.MILLISECONDS)
            .flatMap(new Func1<Long, Observable<Void>>() {
                @Override
                public Observable<Void> call(Long interval) {
                    System.out.println("Writing SSE event for interval: " + interval);
                    return response.writeAndFlush(new ServerSentEvent(String.valueOf(interval), "notification", "hello " + interval));
                }
            })
            ...
}
```

Server side SSE pipeline configuration is created with PipelineConfigurators.<ByteBuf>sseServerConfigurator() method.
Analogously to client counterpart, it injects SSE message encoder into Netty's response processing pipeline,
which converts ServerSideEvent object into text representation following HTML5 standard rules.
RxNetty application is released from dealing explicitly with message encoding/decoding and can use exclusively domain
objects instead.
