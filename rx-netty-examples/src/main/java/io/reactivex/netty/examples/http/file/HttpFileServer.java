package io.reactivex.netty.examples.http.file;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.file.ClassPathURIResolver;
import io.reactivex.netty.protocol.http.server.file.FileRequestHandler;

public class HttpFileServer {
    static final int DEFAULT_PORT = 8103;

    private final int port;

    public HttpFileServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, 
            new FileRequestHandler(
                new ClassPathURIResolver()
                ));
        System.out.println("HTTP file server started...");
        return server;
    }

    public static void main(String[] args) {
        new HttpFileServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
