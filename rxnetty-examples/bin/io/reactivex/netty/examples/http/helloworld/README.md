Overview
========

This is the most simplistic example demonstrating how to execute HTTP GET request with RxNetty and how
to read the response.

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
of transformation operations.

HttpClientResponse object provides header portion of the server reply, and an observable for the reply body.
To make complete request available as an input to a user defined method mergeMap operator is applied first. 
The first argument to the merge map return HTTP reply content observable, while the second one is two argument
function implementation with the first argument being the HttpClientResponse object and the second the reply content.

The very last action applied is defined by doOnTerminate operator. It is executed always, irrespective if request
execution was successful or not.

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

