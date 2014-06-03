RxNetty
=======
[![Build Status](https://netflixoss.ci.cloudbees.com/job/RxNetty-master/badge/icon)](https://netflixoss.ci.cloudbees.com/job/RxNetty-master/)

Reactive Extension (Rx) Adaptor for Netty

Getting Started
==========

The best place to start exploring this library is to look at the class [RxNetty] (https://github.com/Netflix/RxNetty/blob/master/rx-netty/src/main/java/io/reactivex/netty/RxNetty.java)

You can also find some common examples of clients and servers created using RxNetty in the [examples directory] (https://github.com/Netflix/RxNetty/tree/master/rx-netty/src/examples)

A very simple HTTP server example can be found [here] (https://github.com/Netflix/RxNetty/blob/master/rx-netty/src/examples/java/io/reactivex/netty/examples/java/HttpWelcomeServer.java)
and the corresponding HTTP client is [here] (https://github.com/Netflix/RxNetty/blob/master/rx-netty/src/examples/java/io/reactivex/netty/examples/java/HelloHttpClient.java)


Example
==========

```java
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.server.HttpServer;

import java.nio.charset.Charset;

public class RxNettyExample {

    public static void main(String... args) throws InterruptedException {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            try {
                if (request.getPath().equals("/error")) {
                    throw new RuntimeException("forced error");
                }
                response.setStatus(HttpResponseStatus.OK);
                return response.writeStringAndFlush("Path Requested =>: " + request.getPath() + "\n");
            } catch (Throwable e) {
                System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                return response.writeStringAndFlush("Error 500: Bad Request\n");
            }
        });

        server.start();

        HttpClient<ByteBuf, ByteBuf> client = RxNetty.createHttpClient("localhost", 8080);

        client.submit(HttpClientRequest.createGet("/"))
                .flatMap(response -> response.getContent())
                .map(data -> "Client => " + data.toString(Charset.defaultCharset()))
                .toBlocking().forEach(System.out::println);

        client.submit(HttpClientRequest.createGet("/error"))
                .flatMap(response -> response.getContent())
                .map(data -> "Client => " + data.toString(Charset.defaultCharset()))
                .toBlocking().forEach(System.out::println);

        client.submit(HttpClientRequest.createGet("/data"))
                .flatMap(response -> response.getContent())
                .map(data -> "Client => " + data.toString(Charset.defaultCharset()))
                .toBlocking().forEach(System.out::println);

        server.shutdown();
    }
}
```

Outputs:

```
Server => Request: /
Client => Path Requested =>: /

Server => Request: /error
Server => Error [/error] => java.lang.RuntimeException: forced error
Client => Error 500: Bad Request

Server => Request: /data
Client => Path Requested =>: /data
```


## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxnetty%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxnetty</groupId>
    <artifactId>rx-netty</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="com.netflix.rxnetty" name="rx-netty" rev="x.y.z" />
```
and for Gradle:

```groovy
compile 'com.netflix.rxnetty:rx-netty:x.y.z'
```

## Build

To build:

```
$ git clone git@github.com:Netflix/RxNetty.git
$ cd RxNetty/
$ ./gradlew build
```


## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/Netflix/RxNetty/issues).

 
## LICENSE

Copyright 2013 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
