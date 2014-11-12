Overview
========

In this example a GET request is sent to the server, which streams back in response a file
provided as a parameter during startup.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runChunkServer -PtextFile=<some_text_file>
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runChunkClient -Pword=<word_to_count>
```

HTTP client
===========

Here is the snippet from [HttpChunkClient](HttpChunkClient.java):

```java
public int filterWords(final String word) {
    PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> configurator = new HttpClientPipelineConfigurator();

    HttpClient<ByteBuf, ByteBuf> client =
            RxNetty.createHttpClient("localhost", port, configurator);

    int count = client.submit(HttpClientRequest.createGet("/chunkedResponse"))
            .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
                @Override
                public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                    return response.getContent().map(new Func1<ByteBuf, String>() {
                        @Override
                        public String call(ByteBuf content) {
                            return content.toString(Charset.defaultCharset());
                        }
                    });
                }
            })
            .lift(new WordSplitOperator())
            .map(new Func1<String, Integer>() {
                @Override
                public Integer call(String someWord) {
                    return someWord.equals(word) ? 1 : 0;
                }
            })
            .reduce(new Func2<Integer, Integer, Integer>() {
                @Override
                public Integer call(Integer accumulator, Integer value) {
                    return accumulator + value;
                }
            }).toBlocking().last();
    return count;
}
```

Default HTTP client pipeline configuration injects object aggregation handler. It is not suitable when large content
is streamed back to the client, since complete response body would have to be collected first before handing it to
the client side application handler. This would cause higher latency and bigger pressure on system resources utilization.
HttpClientPipelineConfigurator provides basic HTTP request/response encoding/decoding functionality, and thus is
suitable for streamed content handling.

To deal with splitting the streamed text file, a custom WordSplitOperator is created, and injected in the HTTP
response processing workflow. WordSplitOperator is an implementation of RxJava's Observable.Operator interface.
For more information on the latter look [here](https://github.com/Netflix/RxJava/wiki/Implementing-Your-Own-Operators).

HTTP server
===========

Here is the snippet from [HttpChunkServer](HttpChunkServer.java):

```java
public HttpServer<ByteBuf, ByteBuf> createServer() {
    HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
        @Override
        public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
            try {
                final Reader fileReader = new BufferedReader(new FileReader(textFile));
                return createFileObservable(fileReader)
                        .flatMap(new Func1<String, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(String text) {
                                return response.writeStringAndFlush(text);
                            }
                        }).finallyDo(new ReaderCloseAction(fileReader));
            } catch (IOException e) {
                return Observable.error(e);
            }
        }
    });
    System.out.println("HTTP chunk server started...");
    return server;
}
```

On the server side when the GET request arrives, a file observable is created first, which will read chunks of
file and pass it further to its registered observers. It is combined next with HTTP response observable, where the
file chunks are streamed back to the client.