Overview
========

TO enable HTTPS connection, configure ```io.reactivex.netty.pipeline.ssl.SSLEngineFactory``` using
HttpClientBuilder. For testing purposes ```io.reactivex.netty.pipeline.ssl.DefaultFactories``` class is provided
that makes it easy to build SSLEngineFactory instances for client and server endpoints. These are good only for testing environment.

Running
=======

To run the example execute:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSslHelloWorldServer
```

and in another console:

```
$ cd RxNetty/rx-netty-examples
$ ../gradlew runSslHelloWorldClient
```

HTTP client
===========

Here is the snippet from [SslHelloWordClient](SslHelloWorldClient.java):

```java
public HttpResponseStatus sendHelloRequest() throws Exception {
    HttpClient<ByteBuf, ByteBuf> rxClient = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", port)
            .withSslEngineFactory(DefaultFactories.trustAll())
            .build();

    HttpResponseStatus statusCode = rxClient.submit(HttpClientRequest.createGet("/hello"))
    ...
}
}
```

The client code is identical to its non-encrypted counterpart ([HelloWorldClient](../helloworld/README.md)), except
the HTTPClient creation step. The predefined DefaultFactories.trustAll() SSLEngineFactory used in the example
will accept all certificates without validation. It is good only for testing purposes.

HTTP server
===========

Here is the snippet from [SslHelloWordClient](SslHelloWorldServer.java):

```java
public HttpServer<ByteBuf, ByteBuf> createServer() throws CertificateException, SSLException {
    HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(port, new RequestHandler<ByteBuf, ByteBuf>() {
        @Override
        public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
            response.writeStringAndFlush("Welcome!!");
            return response.close();
        }
    }).withSslEngineFactory(DefaultFactories.selfSigned()).build();
    ...
}
```

On the server side, SSLEngineFactory is configured with a temporary self signed certificate/private key generated automatically.
It will be accepted on the client side, since in it is configured to trusts all certificates.
This setup should NEVER be used in the production deployment. Its usage is limited to test code/examples.