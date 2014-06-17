Overview
========

This example is a small modification of [TCP echo](../echo/README.md) example that demonstrates how to setup SSL connection.
More detailed explanation of SSL handling is provided with the [SSL HelloWorld](../../ssl/README.md) example. 

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSslTcpEchoServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSslTcpEchoClient
```
