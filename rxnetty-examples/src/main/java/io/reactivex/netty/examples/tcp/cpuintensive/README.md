Overview
========

An example of how to write a server which does some CPU intensive or Blocking work and hence is not suitable for running
the connection processing in the channel's event loop.
This is achieved by using netty's [`EventExecutorGroup`](https://github.com/netty/netty/blob/master/common/src/main/java/io/netty/util/concurrent/EventExecutorGroup.java) 
as a threadpool.
`RxNetty` makes sure that the [`ConnectionHandler`](https://github.com/Netflix/RxNetty/blob/master/rx-netty/src/main/java/io/reactivex/netty/channel/ConnectionHandler.java)
as well as the subscribers of `ObservableConnection`'s content happens on this executor.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runCpuIntensiveTcpServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runCpuIntensiveTcpClient
```