RxNetty Examples
================

The examples provided are meant to demonstrate how to handle different protocols with RxNetty, as well as to
show some good practices for different usage scenarios.

Examples Catalog
================

Protocol | Example / Test | Description
---------|---------|------------
HTTP | [Hello World](src/main/java/io/reactivex/netty/examples/http/helloworld/README.md)            | Simple HTTP GET client/server implementation.
HTTP | [SSL Hello World](src/main/java/io/reactivex/netty/examples/ssl/README.md)                    | Hello World version with SSL connection.
HTTP | [Simple POST](src/main/java/io/reactivex/netty/examples/http/post/README.md)                  | Simple HTTP POST client/server implementation.
HTTP | [Chunked GET](src/main/java/io/reactivex/netty/examples/http/chunk/README.md)                 | An example of how to handle large, chunked reply that is not pre-aggregated by the default pipline configurator.
HTTP | [Server Side Events](src/main/java/io/reactivex/netty/examples/http/sse/README.md)            | This examples demonstrates how to implement server side event stream, and how to handle it on the client side.
HTTP | [Log tail](src/main/java/io/reactivex/netty/examples/http/logtail/README.md)                  | A more sophisticated server side event example, with multiple event sources and an intermediary aggregating separate data streams.
HTTP | [Word Counter](src/main/java/io/reactivex/netty/examples/http/wordcounter/README.md)          | More complex HTTP POST example demonstrating how to use ContentSource framework  to upload a file onto the server.
TCP  | [Echo Server](src/main/java/io/reactivex/netty/examples/tcp/echo/README.md)                   | A simple echo client.
TCP  | [SSL Echo Server](src/main/java/io/reactivex/netty/examples/tcp/ssl/README.md)                | A simple echo client with SSL connection.
TCP  | [TCP Server Side Event Stream](src/main/java/io/reactivex/netty/examples/tcp/event/README.md) | TCP server side event stream example, with configurable client side processing delay.
TCP  | [Interval](src/main/java/io/reactivex/netty/examples/tcp/interval/README.md)                  | A bit more sophisticated event stream example, with explicit subscribe/unsubscribe control mechanism.
UDP  | [Hello World](src/main/java/io/reactivex/netty/examples/udp/README.md)                        | UDP version of a simple request - reply client/server implementation.

Build
=====

To build:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew build
```

Run
===

All examples can be run from a command line. Most of them accepts some parameters, but usually the defaults are just
fine. The examples have their accompanying unit test classes, which run all endpoints in single JVM process.
Running a test class is thus yet another and convenient way to run a particular example specially within an IDE.
 

It is possible to run example client/server endpoints using Gradle. For list of available tasks check "Examples" group
in the gradle task list:

```
$ ../gradlew tasks
```

For example to run HelloWorld example, start server and client process with the following commands:

```
$ ../gradlew runHelloWorldServer
```

and in another console:

```
$ ../gradlew runHelloWorldClient
```
Some of the examples require parameters. They must be passed with ```-P<name>=<value>``` option. 