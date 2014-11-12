Overview
========

Log tail example aims to illustrate how SOA application, consisting of multiple level service hierarchy could be
implemented with RxNetty. The key service here is log aggregator (LogAggregator class) that taps to multiple
lower-level services (log producers), and processes/filters the received log entries prior to sending it further
to a client.

Running
=======

You can run multiple log producers, but they have to have consecutive port numbers:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runLogTailProducer -Pport=<N> -Pinterval=<interval_in_ms> > logProducer1.log &
$ ../gradlew runLogTailProducer -Pport=<N+1> -Pinterval=<interval_in_ms> > logProducer2.log &
...
$ ../gradlew runLogTailProducer -Pport=<N+M> -Pinterval=<interval_in_ms> > logProducerM.log &
```

next log aggregator has to be started:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runLogTailAggregator -PportFrom=<N> -PportTo=<N+M-1>
```

and the last one is the client:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runLogTailClient
```
