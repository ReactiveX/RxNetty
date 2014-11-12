Overview
========

This example demonstrates how to execute simple POST operation. For more complex scenario of sending
large content with HTTP chunked encoding look [here](../chunk).

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSimplePostServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSimplePostClient
```

HTTP client
===========

Here is the snippet from [SimplePostClient](SimplePostClient.java):

```java
public String postMessage() {
    PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
            = PipelineConfigurators.httpClientConfigurator();

    HttpClient<String, ByteBuf> client = RxNetty.createHttpClient("localhost", port, pipelineConfigurator);

    HttpClientRequest<String> request = HttpClientRequest.create(HttpMethod.POST, "test/post");
    request.withRawContentSource(new RawContentSource.SingletonRawSource<String>(MESSAGE, new StringTransformer()));

    String result = client.submit(request).flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
        @Override
        public Observable<String> call(HttpClientResponse<ByteBuf> response) {
            return response.getContent().map(new Func1<ByteBuf, String>() {
                @Override
                public String call(ByteBuf byteBuf) {
                    return byteBuf.toString(Charset.defaultCharset());
                }
            });
        }
    }).toBlocking().single();

    return result;
}
```
 
HTTP POST request body content is provided via objects implementing ContentSource or RawContentSource interfaces.
ContentSource is an iterator style interface with type parameter. The iterator values are written directly to
Netty's output pipeline, and proper encoding is left to Netty. RawContentSource is an extension of ContentSource
interface that provides a transformer from the original type to Netty's ByteBuf. Thus the encoding is handled at
RxNetty's level.

The client example above utilizes RawContentSource interface. Predefined SingletonRawSource and StringTransformer
are used to create single element RawContentSource object with a transform from String to ByteBuf.

HTTP server
===========

Here is the snippet from [SimplePostServer](SimplePostServer.java):

```java
    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                return request.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                }).reduce("", new Func2<String, String, String>() {
                    @Override
                    public String call(String accumulator, String value) {
                        return accumulator + value;
                    }
                }).flatMap(new Func1<String, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(String clientMessage) {
                        response.writeString(clientMessage.toUpperCase());
                        return response.close();
                    }
                });
            }
        });
        System.out.println("Simple POST server started...");
        return server;
    }
```

The POST request body can be extracted by calling HttpServerRequest.getContent method. The default HTTP request pipeline
includes request content aggregation handler, thus there is a single call to the corresponding handler function
with ByteBuf object containing whole request body. See [chunked encoding](../chunk) example to see how to handle
the content in fragments.