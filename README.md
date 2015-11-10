[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/Netflix/Hystrix.svg)](http://isitmaintained.com/project/Reactivex/RxNetty "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/Netflix/Hystrix.svg)](http://isitmaintained.com/project/Reactivex/RxNetty "Percentage of issues still open")

Branch Status
=======

This is the current development branch for RxNetty and it is __not__ stable, if you are looking for a stable release, you should use the [latest 0.4.x artifacts](https://github.com/ReactiveX/RxNetty/releases).

Motivations
======

Motivations and detailed status of the breaking changes in 0.5.x can be found [here](https://github.com/ReactiveX/RxNetty/wiki/0.5.x-FAQs)
RxNetty
=======
<a href='https://travis-ci.org/ReactiveX/RxNetty/builds'><img src='https://travis-ci.org/ReactiveX/RxNetty.svg?branch=0.5.x'></a>

Reactive Extension (Rx) Adaptor for Netty

Getting Started
==========

The best place to start exploring this library is to look at the [examples] (rxnetty-examples) for some common usecases addressed by RxNetty.

A very simple HTTP server example can be found [here] (rxnetty-examples/src/main/java/io/reactivex/netty/examples/http/helloworld/HelloWorldServer.java)
and the corresponding HTTP client is [here] (rxnetty-examples/src/main/java/io/reactivex/netty/examples/http/helloworld/HelloWorldClient.java)

## Binaries

The binaries for 0.5.x are only available as snapshots at [jfrog artifactory](https://oss.jfrog.org/webapp/search/artifact/?1&q=RxNetty), the following snippet can be used to refer to these artifacts with gradle:

```groovy
repositories {
    maven { url 'https://oss.jfrog.org/libs-snapshot' }
}
```
##### For HTTP
```groovy
compile 'io.reactivex:rxnetty-http:0.5.0-SNAPSHOT'
```
###### Unintentional release artifacts

There are two artifacts in maven central 0.5.0 and 0.5.1 which were unintentionally released from 0.4.x branch. Do not use them. [More details here](https://github.com/ReactiveX/RxNetty/issues/439)

## Build

To build:

```
$ git clone https://github.com/ReactiveX/RxNetty.git -b 0.5.x
$ cd RxNetty/
$ ./gradlew build
```


## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/ReactiveX/RxNetty/issues).


## LICENSE

Copyright 2014 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
