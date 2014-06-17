Overview
========

RxNetty supports SSL protocol with server side certificate validation. Two modes are provided:
* insecure
* trusted server

Insecure mode trusts all X.509 certificates without any verification on client side, and on server side generates 
self signed certificate per each RxServer instance created.
   
In trusted mode, the client can be configured with application or standard Java SDK trust store. On server side,
certificate chain file and private key (optionally protected with password) must be configured.

SSL support comes with a set of convenience factory methods in ```RxServer``` and a set of predefined pipeline configurators
in ```PipelineConfigurators``` class. For more complex configurations or non-typical use cases 
```ClientSslPipelineConfiguratorBuilder``` and ```ServerSslPipelineConfiguratorBuilder``` builders can be used.
One example of that would be creating server side pipeline configurator for trusted server mode. 

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
    HttpClient<ByteBuf, ByteBuf> rxClient = RxNetty.createSslInsecureHttpClient("localhost", port);

    HttpResponseStatus statusCode = rxClient.submit(HttpClientRequest.createGet("/hello"))
    ...
}
}
```

The client code is identical to its non-encrypted counterpart ([HelloWorldClient](../helloworld/README.md)), except
the HTTPClient creation step. In this example, the predefined RxNetty.createSslInsecureHttpClient method is used
which configures SSH handler in the pipeline with trust-all trust store. 

HTTP server
===========

Here is the snippet from [SslHelloWordClient](SslHelloWorldServer.java):

```java
    public HttpServer<ByteBuf, ByteBuf> createServer() throws CertificateException, SSLException {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createSslInsecureHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
        ...
    }
```

On the server side, a convenience factory method is again used to create HTTPServer with SSL handler.
In the insecure mode, a self signed certificate will be automatically generated and used for authentication.
It will be accepted on the client side, since in the insecure mode client trusts all certificates.
The insecure mode should NEVER be used in the production deployment. Its usage is limited to test code/examples.