TCP Examples
==============

The following TCP examples are available:

- __Echo__: A TCP "Hello World" example that sends a string "Hello World!" to the target server and expects an echo 
response. This example constitutes of an [Echo Client](src/main/java/io/reactivex/netty/examples/tcp/echo/EchoClient.java)
and an [Echo Server](src/main/java/io/reactivex/netty/examples/tcp/echo/EchoServer.java)


- __Secure Echo__: Same as the echo example but uses TLS secured connections. This example constitutes of a 
[Secure Echo Client](src/main/java/io/reactivex/netty/examples/tcp/secure/SecureEchoClient.java)
and a [Secure Echo Server](src/main/java/io/reactivex/netty/examples/tcp/secure/SecureEchoServer.java)


- __Simple Interception__: A example that demonstrates how to write basic interceptors for TCP server and client.
This example constitutes of an [Intercepting Client](src/main/java/io/reactivex/netty/examples/tcp/interceptors/simple/InterceptingClient.java)
and an [Intercepting Server](src/main/java/io/reactivex/netty/examples/tcp/interceptors/simple/InterceptingServer.java)


- __Transformation Interception__: A example that demonstrates how to write interceptors that modify the types of content
for HTTP server and client.
This example constitutes of an [Intercepting Client](src/main/java/io/reactivex/netty/examples/tcp/interceptors/transformation/InterceptingClient.java)
and an [Intercepting Server](src/main/java/io/reactivex/netty/examples/tcp/interceptors/transformation/TransformingInterceptorsServer.java)


- __Streaming__: A TCP streaming example where the server sends an infinite stream of data and the client taps to this 
infinite stream to get as many messages as desired. This example constitutes of a 
[Streaming Client](src/main/java/io/reactivex/netty/examples/tcp/streaming/StreamingClient.java)
and a [Streaming Server](src/main/java/io/reactivex/netty/examples/tcp/streaming/StreamingServer.java)


- __Proxy__: A TCP proxy example where a proxy server proxies all connections to a specific target server. This example 
constitutes of a [Proxy Client](src/main/java/io/reactivex/netty/examples/tcp/proxy/ProxyClient.java)
and a [Proxy Server](src/main/java/io/reactivex/netty/examples/tcp/proxy/ProxyServer.java)


- __Load balancing__: A TCP load balancing example where the client load-balances between a set of hosts and also 
demonstrates how to write failure detection logic to detect unhealthy hosts. This example constitutes of a 
[Load Balancing Client](src/main/java/io/reactivex/netty/examples/tcp/loadbalancing/TcpLoadBalancingClient.java)
and a [Load Balancer](src/main/java/io/reactivex/netty/examples/tcp/loadbalancing/TcpLoadBalancer.java)

- __Simple interceptor__: A simple interceptor for TCP server to demonstrate sending an initial hello message before
the actual connection handling starts. This example constitutes of an 
[Intercepting Server](src/main/java/io/reactivex/netty/examples/tcp/interceptors/simple/InterceptingServer.java)
and an [Intercepting Client](src/main/java/io/reactivex/netty/examples/tcp/interceptors/simple/InterceptingClient.java)

- __Interceptors with transformation__: An interceptor for TCP server to demonstrate transformation of input and output
on a connection. This example constitutes of an 
[Intercepting Server](src/main/java/io/reactivex/netty/examples/tcp/interceptors/simple/InterceptingServer.java)
and an [Intercepting Client](src/main/java/io/reactivex/netty/examples/tcp/interceptors/simple/InterceptingClient.java)



