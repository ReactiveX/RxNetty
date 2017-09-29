RxNetty
=======
<a href='https://travis-ci.org/ReactiveX/RxNetty/builds?branch=0.6.x'><img src='https://travis-ci.org/ReactiveX/RxNetty.svg?branch=0.6.x'></a>

Reactive Extension (Rx) Adaptor for Netty

Example
==========

```java
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import io.reactivex.netty.http.client.HttpClient;
import io.reactivex.netty.http.server.HttpServer;

import java.nio.charset.Charset;

public final class RxNettyExample {

  public static void main(String... args) throws InterruptedException {
    NettyContext context = HttpServer.create(8080).newHandler((request, response) -> {
      System.out.println("Server => Request: " + request.uri());
      try {
        if ("/error".equals(request.uri())) {
          throw new RuntimeException("forced error");
        }
        response.status(HttpResponseStatus.OK);
        return response.sendString(Flowable.just("Path Requested =>: " + request.uri() + '\n'));
      } catch (Throwable e) {
        System.err.println("Server => Error [" + request.uri() + "] => " + e);
        response.status(HttpResponseStatus.BAD_REQUEST);
        return response.sendString(Flowable.just("Error 500: Bad Request\n"));
      }
    }).blockingSingle();

    HttpClient.create(8080)
        .get("/")
        .flatMapMaybe(response -> response.receive().aggregate())
        .map(data -> "Client => " + data.toString(Charset.defaultCharset()))
        .blockingForEach(System.out::println);

    HttpClient.create(8080)
        .get("/error", request -> request.failOnClientError(false))
        .flatMapMaybe(response -> response.receive().aggregate())
        .map(data -> "Client => " + data.toString(Charset.defaultCharset()))
        .blockingForEach(System.out::println);

    HttpClient.create(8080)
        .get("/data")
        .flatMapMaybe(response -> response.receive().aggregate())
        .map(data -> "Client => " + data.toString(Charset.defaultCharset()))
        .blockingForEach(System.out::println);

    context.dispose();
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

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search|ga|1|io.reactivex.rxnetty).

Example for Maven:

```xml
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxnetty</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="io.reactivex" name="rxnetty" rev="x.y.z" />
```
and for Gradle:

```groovy
compile 'io.reactivex:rxnetty:x.y.z'
```

## Build

To build:

```
$ git clone https://github.com/ReactiveX/RxNetty.git -b 0.6.x
$ cd RxNetty/
$ ./gradlew build
```


## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/ReactiveX/RxNetty/issues).

## Current development branch

[0.6.x](https://github.com/ReactiveX/RxNetty/tree/0.6.x) is the current development branch.

## LICENSE

Copyright (c) 2017 RxNetty Contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
