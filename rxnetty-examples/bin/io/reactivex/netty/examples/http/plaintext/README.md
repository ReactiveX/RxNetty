Overview
========

This example is intended to be a sample used for "helloworld" performance tests for RxNetty and has optimizations specific
for helloworld kind of tests.

The following are the primary differences over the [HelloWorld example](../helloworld) 

- This example does NOT aggregate HTTP requests.
- This example does NOT print the request headers.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runPlainTextServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runPlainTextClient
```