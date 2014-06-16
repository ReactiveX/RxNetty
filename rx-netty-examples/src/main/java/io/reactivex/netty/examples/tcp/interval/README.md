Overview
========

This example illustrates simple client/server protocol implementation. The protocol sequence includes the following
steps:

```
client                     server
     | subscribe           |
     |-------------------->|
     |             event 1 |
     |<--------------------|
     |             event 2 |
     |<--------------------|
     |                 ... |
     |             event N |
     |<--------------------|
     | unsubscribe         |
     |-------------------->|
     |                     |
```

On the client side first subscribe event is sent, which triggers sending of unbounded sequence of events from
server at configured interval. The client consumes a configurable number of events and terminates. Alternatively
it could send unsubscribe event to server which would result in connection termination on the server side.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runTcpIntervalServer -Pinterval=<sending_interval_in_ms>
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runTcpIntervalClient -P<events=no_of_events>
```
