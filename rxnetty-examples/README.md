
This module contains examples for various usecases that `RxNetty` to act as a "Getting Started" guide to use `RxNetty`.
The examples are not exhaustive in terms of the features `RxNetty` provides but are representative of different 
usecases that one can achieve using `RxNetty`  

Catalog
================

The catalog is categorized with protocol as well as the user level like Beginner, Intermediate, Advanced, for you to get
to the correct example when using `RxNetty`. The following protocol based categorization links to the elaborate examples
per protocol.

###TCP

[This catalog](TCP.md) contains all examples for TCP.

###HTTP

[This catalog](HTTP.md) contains all examples for HTTP.

###WebSockets

[This catalog](WS.md) contains all examples for HTTP.

Using the examples
===============

All examples have a `Server` and `Client` class, both of which can be run independently from their `main` methods.

#####Server

All servers use ephemeral ports and when started outputs the port it is using. The server code is usually useful when
you are trying to understand how to write a server for that usecase. If you are only interested in the client, then you
can safely ignore the server part as the client is standalone.

#####Client

All clients can be executed in 3 different ways:

- __Default__: This internally starts the server required by the client and uses the ephermal port used by the server. 
This is the easiest way to run any example.


- __Use an already started server__: In this mode, the client does not try to start a server by itself. For running in
this mode, you should pass the port as a program argument while running the client, eg:

    ```
    java io.reactivex.netty.examples.http.helloworld.HelloWorldClient [server port]
    ```

- __Use an external server__: In this mode, the client does not try to start a server by itself. For running in
this mode, you should pass server host & port as a program argument while running the client, eg:

    ```
     java io.reactivex.netty.examples.http.helloworld.HelloWorldClient [server port] [server host]
    ```