# RxNetty Servo Plugin

This plugin provides metrics support for RxNetty using [servo](https://github.com/Netflix/servo)
 
### Usage

#### Global registration

`RxNetty` provides a way to register servo metrics module for all clients and servers created by `RxNetty`. This can be 
achieved by the following code:

 ```java
 
 import io.reactivex.netty.RxNetty;
 import io.reactivex.netty.servo.ServoEventsListenerFactory;
 
 public class MyApplication {
 
     public static void main(String[] args) {
         RxNetty.useMetricListenersFactory(new ServoEventsListenerFactory());
     }
 }
 
 ```

#### Instance registrations
 
 If for some reason, global registration is not appropriate, one can register the listeners returned by 
 `ServoEventsListenerFactory` on a instance of server or client. The following sample code demonstrates how:
 
 ```java
 
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return Observable.empty();
            }
        });
        
        ServoEventsListenerFactory factory = new ServoEventsListenerFactory();
        
        HttpServerListener listener = factory.forHttpServer(server);
        
        server.subscribe(listener);
 ```
  
##### Multiple registrations
  
It is perfectly valid for the same `MetricEventsListener` to be registered with multiple client or server instances. In
 such a case, one can get aggregated metrics over the clients or servers with which this listener is registered.
 The following sample code demonstrates how:
 
 ```java
 
        HttpClient<ByteBuf, ByteBuf> client = RxNetty.createHttpClient("localhost", 7777);
        ServoEventsListenerFactory factory = new ServoEventsListenerFactory();
        HttpClientListener listener = factory.forHttpClient(client);
        client.subscribe(listener);
        RxNetty.createHttpClient("localhost", 7778).subscribe(listener);
 ```
 
### Available Metrics
 
#### Server

##### TCP
 
 * **Live Connections**: The number of open connections from all clients to this server. This is a gauge.
 * **Inflight Connections**: The number of connections that are currently being processed by this server.  This is a gauge.
 * **Failed Connections**: The number of times that connection handling failed.  This is a monotonically increasing counter.
 * **Connection processing times**: Time taken for processing a connection.
 * **Pending connection close**: Number of connections which are requested to be closed but are not yet closed. This is a gauge. 
 * **Failed connection close**: Number of times when the connection close failed. This is a monotonically increasing counter.
 * **Connection close times**: Time taken for closing a connection.
 * **Pending Writes**: Writes that are pending to be written over the socket. This includes writes which are not flushed. 
 This is a gauge.
 * **Pending Flushes**: Flushes that are issued but are not over yet. This is a gauge.
 * **Bytes Written**: Total number of bytes written. This is a monotonically increasing counter.
 * **Write Times**: The time taken to finish a write.
 * **Bytes Read**: The total number of bytes read. This is a monotonically increasing counter.
 * **Failed Writes**: The total number of writes that failed. This is a monotonically increasing counter.
 * **Failed Flushes**: The total number of flushes that failed. This is a monotonically increasing counter.
 * **Flush times**: The time taken to finish a flush. 
 
##### HTTP
 
 HTTP contains all the metrics that are available from TCP. The following metrics are specific to HTTP:
  
* **Request backlog**: The number of requests that have been received but not started processing. This is a gauge. 
* **Inflight requests**: The number of requests that have been started processing but not yet finished processing. This is a gauge.
* **Response Write Times**: Time taken to write responses, including headers and content.
* **Request Read Times**: Time taken to read a request.
* **Processed Requests**: Total number of requests processed. This is a monotonically increasing counter.
* **Failed Requests**: Total number of requests for which the request handling failed.
* **Failed response writes**: Total number of responses for which the writes failed.
 
##### UDP

UDP contains all the metrics that are available from TCP.

#### Client

##### TCP
 
 * **Live Connections**: The number of open connections from this clients to a server. This is a gauge.
 * **Connection count**: The total number of connections ever created by this client.  This is a monotonically increasing counter.
 * **Pending Connections**: The number of connections that are pending. This is a gauge.
 * **Failed connects**: Total number of connect failures.
 * **Connection times**: Time taken to establish a connection.
 * **Pending connection close**: Number of connections which are requested to be closed but are not yet closed. This is a gauge. 
 * **Failed connection close**: Number of times when the connection close failed. This is a monotonically increasing counter.
 * **Pending pool acquires**: For clients with a connection pool, the number of acquires that are pending. This is a gauge. 
 * **Failed pool acquires**: For clients with a connection pool, the number of acquires that failed. This is a monotonically increasing counter.
 * **Pool acquire times**: For clients with a connection pool, time taken to acquire a connection from the pool.
 * **Pending pool releases**: For clients with a connection pool, the number of releases that are pending. This is a gauge. 
 * **Failed pool releases**: For clients with a connection pool, the number of releases that failed. This is a monotonically increasing counter.
 * **Pool releases times**: For clients with a connection pool, time taken to release a connection to the pool.
 * **Pool acquires**: For clients with a connection pool, the total number of acquires from the pool.
 * **Pool evictions**: For clients with a connection pool, the total number of evictions from the pool.
 * **Pool reuse**: For clients with a connection pool, the total number of times a connection from the pool was reused.
 * **Pool releases**: For clients with a connection pool, the total number of releases to the pool.
 * **Pending Writes**: Writes that are pending to be written over the socket. This includes writes which are not flushed. 
 This is a gauge.
 * **Pending Flushes**: Flushes that are issued but are not over yet. This is a gauge.
 * **Bytes Written**: Total number of bytes written. This is a monotonically increasing counter.
 * **Write Times**: The time taken to finish a write.
 * **Bytes Read**: The total number of bytes read. This is a monotonically increasing counter.
 * **Failed Writes**: The total number of writes that failed. This is a monotonically increasing counter.
 * **Failed Flushes**: The total number of flushes that failed. This is a monotonically increasing counter.
 * **Flush times**: The time taken to finish a flush. 
 
##### HTTP
 
 HTTP contains all the metrics that are available from TCP. The following metrics are specific to HTTP:
  
* **Request backlog**: The number of requests that have been submitted but not started processing. This is a gauge. 
* **Inflight requests**: The number of requests that have been started processing but not yet finished processing. This is a gauge.
* **Processed Requests**: Total number of requests processed. This is a monotonically increasing counter.
* **Request Write Times**: Time taken to write requests, including headers and content.
* **Response Read Times**: Time taken to read a response.
* **Failed Responses**: Total number of responses that failed i.e. for which the requests were sent but response was an error.
* **Failed request writes**: Total number of requests for which the writes failed.
 
##### UDP

UDP contains all the metrics that are available from TCP.
