Overview
========

In this example a GET request is sent to the server, which streams back in response a file
provided as a parameter during startup.

Running
=======

To run the example execute:

```
$ cd RxNetty/rxnetty-examples
$ ../gradlew runChunkServer -PtextFile=<some_text_file>
```

and in another console:

```
$ cd RxNetty/rxnetty-examples
$ ../gradlew runChunkClient -Pword=<word_to_count>
```

HTTP client
===========

Here is the snippet from [HttpChunkClient](HttpChunkClient.java):

```java
public int filterWords(final String word) {
    PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> configurator =
            new HttpClientPipelineConfigurator<ByteBuf, ByteBuf>();

    HttpClient<ByteBuf, ByteBuf> client =
            RxNetty.createHttpClient("localhost", port, configurator);

    return client.submit(HttpClientRequest.createGet("/chunkedResponse"))
            .flatMap(response -> {
                return response.getContent().map((ByteBuf content) -> {
                    return content.toString(Charset.defaultCharset());
                });
            })
            .lift(new WordSplitOperator())
            .map(someWord -> someWord.equals(word) ? 1 : 0)
            .reduce((Integer accumulator, Integer value) -> {
                return accumulator + value;
            }).toBlocking().last();
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
    return RxNetty.createHttpServer(port, (request, response) -> {
        return StringObservable.using(() -> new FileReader(textFile), (reader) -> StringObservable.from(reader))
                .flatMap(text -> response.writeStringAndFlush(text));
    });
}
```

On the server side when the GET request arrives, a file observable is created first, which will read chunks of
file and pass it further to its registered observers. It is combined next with HTTP response observable, where the
file chunks are streamed back to the client.