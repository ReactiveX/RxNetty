HTTP Examples
==============

The following HTTP examples are available:

- Hello World: A "Hello World" example that demonstrates how to write basic HTTP server and client. 
This example constitutes of an [Hello Client](src/main/java/io/reactivex/netty/examples/http/helloworld/HelloWorldClient.java)
and an [Hello Server](src/main/java/io/reactivex/netty/examples/http/helloworld/HelloWorldServer.java)


- Secure Hello World: A "Hello World" example that demonstrates how to write basic HTTPS server and client. 
This example constitutes of an [Hello Client](src/main/java/io/reactivex/netty/examples/http/secure/SecureHelloWorldClient.java)
and an [Hello Server](src/main/java/io/reactivex/netty/examples/http/secure/SecureHelloWorldServer.java)


- Performance benchmark hello world: This example is specifically made for many "Hello World" benchmarks that are done
to evaluate framework overheads. As these benchmarks tend to be testing the performance when the server is I/O bound, 
 this example essentially contains all the micro-optimizations that remove application level overheads.
[Perf Client](src/main/java/io/reactivex/netty/examples/http/perf/PerfHelloWorldClient.java)
and a [Perf Server](src/main/java/io/reactivex/netty/examples/http/perf/PerfHelloWorldServer.java)


- Proxy: An HTTP proxy example where a proxy server proxies all requests to a specific target server. This example 
constitutes of a [Proxy Client](src/main/java/io/reactivex/netty/examples/http/proxy/ProxyClient.java)
and a [Proxy Server](src/main/java/io/reactivex/netty/examples/http/proxy/ProxyServer.java)


- Load balancing: An HTTP load balancing example where the client load-balances between a set of hosts and also 
demonstrates how to write failure detection logic to detect unhealthy hosts. This example constitutes of a 
[Load Balancing Client](src/main/java/io/reactivex/netty/examples/http/loadbalancing/HttpLoadBalancingClient.java)
and a [Load Balancer](src/main/java/io/reactivex/netty/examples/http/loadbalancing/HttpLoadBalancer.java)


- Server Sent Events: An example to demonstrate how to write basic [Server Sent Events](http://www.w3.org/TR/eventsource)
 client and server. This example constitutes of a 
 [Client](src/main/java/io/reactivex/netty/examples/http/sse/HelloSseClient.java)
and a [Server](src/main/java/io/reactivex/netty/examples/http/sse/HelloSseServer.java)


- Streaming: An example to demonstrate how to write basic HTTP streaming client and server, where the server sends an
infinite stream of HTTP chunks and the client reads a pre-defined number of these chunks. This example constitutes of a 
 [Streaming Client](src/main/java/io/reactivex/netty/examples/http/streaming/StreamingClient.java)
and a [Streaming Server](src/main/java/io/reactivex/netty/examples/http/sse/StreamingServer.java)

