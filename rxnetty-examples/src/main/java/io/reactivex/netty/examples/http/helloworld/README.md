Overview
========

This is a simple and raw example demonstrating how to execute HTTP GET request with RxNetty and how
to read the response working directly with ByteBufs. 

Typically IPC/RPC libraries would build further abstractions with serialization and protocols on top
of this but this shows the basic request/response of HTTP. 

Running
=======

To run the example execute:

```
$ cd RxNetty/rxnetty-examples
$ ../gradlew runHelloWorldServer
```

and in another console:

```
$ cd RxNetty/rxnetty-examples
$ ../gradlew runHelloWorldClient
```

HTTP client
===========

Here is the snippet from [HelloWordClient](HelloWorldClient.java):

```java
public String sendHelloRequest() throws InterruptedException, ExecutionException, TimeoutException {
    return RxNetty.createHttpGet("http://localhost:" + port + "/hello")
            .flatMap(response -> {
                printResponseHeader(response);
                return response.getContent().<String> map(content -> {
                    return content.toString(Charset.defaultCharset());
                });
            })
            .toBlocking()
            .toFuture().get(1, TimeUnit.MINUTES);
}
```

io.reactivex.netty.RxNetty class provides a rich set of factory methods to create RxNetty clients and servers for
all the supported protocols. In this example RxNetty.createHttpGet method creates HttpClientRequest instance
with the given URI, and submits a request to the server. The return value from this method is an Observable of
HttpClientResponse values (only one in this scenario). The complete processing flow is implemented as a set
of transformation operations using `flatMap` and `map`.

RxNetty is fully asynchronous framework, but sometimes it is convenient to make execution synchronous. It can be 
accomplished by converting an Observable to BlockingObservable using toBlocking operator, like in the code
snippet above. There are multiple methods to get value from a BlockingObservable (first, last, etc). 
In this particular case we have just one value (HttpResponseStatus). 


HTTP server
===========

Here is the snippet from [HelloWordServer](HelloWorldServer.java):

```java
public HttpServer<ByteBuf, ByteBuf> createServer() {
    return RxNetty.createHttpServer(port, (request, response) -> {
        printRequestHeader(request);
        return response.writeStringAndFlush("Hello World!");
    });
}

```

The `(request, response)` handler returns an `Observable<Void>` so the entire handler is async and can 
signal when it completes successfully (via `onCompleted`) or with an error (via `onError`).

The writes can be done with or without flushing. In this case for simplicity we `writeAndFlush`. 
There are also configurators for serializing to/from `ByteBuf` but when dealing with strings there
are simple overloads such as `writeStringAndFlush` to take care of the common case. 