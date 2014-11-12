Overview
========

This is more complex POST example where custom RawContentSource class is implemented to stream a file content 
to the server, which provides word counting service, and sends back number of counted words as a reply.
This example is a reverse of the [chunk](../chunk) example, where file content was sent from server to client, which performed
word counting operations on it.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runWordCounterServer 
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runWordCounterClient -PtextFile=<some_text_file>
```

HTTP client
===========

Here is the snippet from [WordCounterClient](WordCounterClient.java):

```java
public int countWords() throws IOException {
    PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
            = PipelineConfigurators.httpClientConfigurator();

    HttpClient<String, ByteBuf> client = RxNetty.createHttpClient("localhost", port, pipelineConfigurator);
    HttpClientRequest<String> request = HttpClientRequest.create(HttpMethod.POST, "test/post");

    FileContentSource fileContentSource = new FileContentSource(new File(textFile));
    request.withRawContentSource(fileContentSource);

    WordCountAction wAction = new WordCountAction();
    client.submit(request).toBlocking().forEach(wAction);

    fileContentSource.close();

    return wAction.wordCount;
}
```

WordCounterClient.FileContentSource class in the code above is a custom implementation of RawContentSource interface, 
that reads text file in small chunks. It uses the RxNetty provided StringTransformer for String to ByteBuf converstion.


HTTP server
===========

Here is the snippet from [WordCounterServer](WordCounterServer.java):

```java
    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                return request.getContent()
                        .map(new Func1<ByteBuf, String>() {
                            @Override
                            public String call(ByteBuf content) {
                                return content.toString(Charset.defaultCharset());
                            }
                        })
                        .lift(new WordSplitOperator())
                        .count()
                        .flatMap(new Func1<Integer, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(Integer counter) {
                                response.writeString(counter.toString());
                                return response.close();
                            }
                        });
            }
        });
        System.out.println("Started word counter server...");
        return server;
    }
```

The server is doing simple word splitting/counting similarly to the [chunk](../chunk) example.
