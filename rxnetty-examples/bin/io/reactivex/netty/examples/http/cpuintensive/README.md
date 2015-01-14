Overview
========

An example of how to write a server which does some CPU intensive or Blocking work and hence is not suitable for running
the request processing in the channel's event loop.
This is achieved by using netty's [`EventExecutorGroup`](https://github.com/netty/netty/blob/master/common/src/main/java/io/netty/util/concurrent/EventExecutorGroup.java) 
as a threadpool.
`RxNetty` makes sure that the [`RequestHandler`](https://github.com/Netflix/RxNetty/blob/master/rxnetty/src/main/java/io/reactivex/netty/protocol/http/server/RequestHandler.java)
as well as the subscribers of `HttpServerRequest`'s content happens on this executor.

Running
=======

To run the example execute:

```
$ cd RxNetty/rxnetty-examples
$ ../gradlew runCpuIntensiveHttpServer
```

and in another console:

```
$ cd RxNetty/rxnetty-examples
$ ../gradlew runCpuIntensiveHttpClient
```
