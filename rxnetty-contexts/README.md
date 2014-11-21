# Infrastructure Contexts

### Overview

This module of RxNetty handles transparent propagation of infrastructure contexts (eg: tracing information) across
service boundaries. Any object that can provide a mechanism to serialze/de-serialize to/from a String, can be used as
a context. However, it is recommended that any business specific information should not be passed using these contexts.
Instead, your APIs should have explicit contract with the clients to pass this information as parameters and not
transparently across service boundaries.


### Multi-protocol support

This module is designed to be used for any kind of protocol that RxNetty supports. It provides a plugin model to hook
different protocols. Out of the box, we have implemented the plugin for HTTP.

### Usage

The factory class [RxContexts](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/RxContexts.java)
is provided to create Client and Servers with this feature enabled. The easiest way to use this feature is to start from
this class. This factory class similar to RxNetty core, uses another factory [ContextPipelineConfigurators](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/ContextPipelineConfigurators.java)
for netty's pipeline configurations. If RxContexts does not fit your needs, the ContextPipelineConfigurators can be
 used to configure netty's pipelines for your clients and servers.
 Since, these factory methods return the first class RxNetty clients and servers, the usage after creating these
 instances is identical to the normal RxClient and RxServer.

### Concepts

- _**Request**_: Any infrastructure context is always associated with a unique request. The concept of a request does not
necessarily mean that the end user has to send one request at a time on a connection. Out of the box HTTP plugin supports
HTTP pipelining. [RequestCorrelator](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/RequestCorrelator.java)
and [RequestIdProvider](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/RequestIdProvider.java)
interfaces together provides the required pluggability to define the "request" abstraction.

- _**Context Container**_: All contexts for a request is contained in a [ContextsContainer](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/ContextsContainer.java)
All operations for managing a context is provided via this interface.

- _**Context Capturing**_: Since a request processing may involve multiple threads, ThreadLocals are not always a good
way to capture these contexts in the application layer (from when a request is received and if an outbound call to other
services is made). So, this module provides an abstraction [ContextCapturer](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/ContextCapturer.java)
to capture these contexts within the application boundaries.

- _**Context Keys**_: Since there can be multiple contexts per request, every context has a locally unique key (unique
within the request). Each of these contexts are passed between services as these key-value pairs. Since, how these pairs
are passed over the wire is governed by the protocol itself, we have defined an abstraction [ContextKeySupplier](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/io/reactivex/netty/contexts/ContextKeySupplier.java)
to define this contract. In HTTP, these pairs are passed as headers by the default plugin.

- _**Bidrectional Contexts**_: By default all contexts are uni-directional i.e. any change made to the context in the
downstream dependencies, is not available inside the upstream services. However, if your context is to be made available
to upstream services (after the downstream service call ends) you can mark your context with the annotation
[BiDirectional](https://github.com/Netflix/RxNetty/blob/master/rxnetty-contexts/src/main/java/com/netflix/server/context/BiDirectional.java)

### Practical usage

- Although, one can do enough work to use this facility directly from RxNetty, it will be much easier if used via [karyon](https://github.com/Netflix/karyon) and [ribbon](https://github.com/Netflix/ribbon)