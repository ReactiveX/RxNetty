TCP Examples
==============

The following TCP examples are available:

- Echo: A TCP "Hello World" example that sends a string "Hello World!" to the target server and expects an echo 
response. This example constitutes of an [Echo Client](src/main/java/io/reactivex/netty/examples/tcp/echo/EchoClient.java)
and an [Echo Server](src/main/java/io/reactivex/netty/examples/tcp/echo/EchoServer.java)


- Secure Echo: Same as the echo example but uses TLS secured connections. This example constitutes of a 
[Secure Echo Client](src/main/java/io/reactivex/netty/examples/tcp/secure/SecureEchoClient.java)
and a [Secure Echo Server](src/main/java/io/reactivex/netty/examples/tcp/secure/SecureEchoServer.java)


- Streaming: A TCP streaming example where the server sends an infinite stream of data and the client taps to this 
infinite stream to get as many messages as desired. This example constitutes of a 
[Streaming Client](src/main/java/io/reactivex/netty/examples/tcp/streaming/StreamingClient.java)
and a [Streaming Server](src/main/java/io/reactivex/netty/examples/tcp/streaming/StreamingServer.java)


- Proxy: A TCP proxy example where a proxy server proxies all connections to a specific target server. This example 
constitutes of a [Proxy Client](src/main/java/io/reactivex/netty/examples/tcp/proxy/ProxyClient.java)
and a [Proxy Server](src/main/java/io/reactivex/netty/examples/tcp/proxy/ProxyServer.java)


- Load balancing: A TCP load balancing example where the client load-balances between a set of hosts and also 
demonstrates how to write failure detection logic to detect unhealthy hosts. This example constitutes of a 
[Load Balancing Client](src/main/java/io/reactivex/netty/examples/tcp/loadbalancing/TcpLoadBalancingClient.java)
and a [Load Balancer](src/main/java/io/reactivex/netty/examples/tcp/loadbalancing/TcpLoadBalancer.java)



