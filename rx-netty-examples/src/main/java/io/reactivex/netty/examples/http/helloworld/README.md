Overview
========

This is the most simplistic example demonstrating how to execute HTTP GET request with RxNetty and how
to read the response.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runHelloWorldServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runHelloWorldClient
```

HTTP client
===========

Here is the snippet from [HelloWordClient](HelloWorldClient.java):

```java
public HttpResponseStatus sendHelloRequest() {
    HttpResponseStatus statusCode = RxNetty.createHttpGet("http://localhost:" + port + "/hello")
            .mergeMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                @Override
                public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
                    return response.getContent();
                }
            }, new Func2<HttpClientResponse<ByteBuf>, ByteBuf, HttpResponseStatus>() {
                @Override
                public HttpResponseStatus call(HttpClientResponse<ByteBuf> response, ByteBuf content) {
                    printResponseHeader(response);
                    System.out.println(content.toString(Charset.defaultCharset()));
                    return response.getStatus();
                }
            })
            .doOnTerminate(new Action0() {
                @Override
                public void call() {
                    System.out.println("=======================");
                }
            }).toBlocking().last();

    return statusCode;
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

Here is the snippet from [HelloWordClient](HelloWorldServer.java):

```java
public HttpServer<ByteBuf, ByteBuf> createServer() {
    HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(port, new RequestHandler<ByteBuf, ByteBuf>() {
        @Override
        public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
            printRequestHeader(request);
            response.writeString("Welcome!!");
            return response.close();
        }
    }).pipelineConfigurator(PipelineConfigurators.<ByteBuf, ByteBuf>httpServerConfigurator()).build();

    System.out.println("HTTP hello world server started...");
    return server;
}
```
RxNetty.newHttpServerBuilder static method creates an instance of HttpServerBuilder object, that following the builder 
pattern, allows for flexible HTTP server configuration. The request handler object is analogous to the its client
side counter part, and does not require any additional explanation.

The PipelineConfigurator is a wrapper for Netty channel pipeline. In this particular case, a predefined
HTTP server pipeline is used which decodes/encodes HTTP request/response objects, and additionally performs
request content aggregation.