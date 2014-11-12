Overview
========

In this example TCP server sends a sequence of events to the client at 10ms intervals. The client is started
with configurable processing delay and a number of events to collect. Once the required number of events is collected
client terminates. 

By setting client side processing delay to larger value then server side send interval, there is an unbounded amount
of unfinished work accumulated in the transport layer, which will eventually result in server crashing with
OutOfMemory exception.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runTcpEventStreamServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runTcpEventStreamClient -Pdelay=<delay_in_ms> -Pevents=<no_of_events>
```
