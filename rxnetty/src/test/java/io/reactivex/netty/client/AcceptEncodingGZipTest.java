package io.reactivex.netty.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;

/**
 * This unit test fires up a client and server and then tests that the client can request gzip content from the server.
 * @author Tom Haggie
 */
public class AcceptEncodingGZipTest {

    private static final String MESSAGE = "Hello world!";

    private int port;
    private HttpServer<ByteBuf, ByteBuf> server;
    private HttpClient<ByteBuf, ByteBuf> client;

    @Before
    public void setupServer() {
        server = createServer();
        server.start();
        port = server.getServerPort();
        client = createClient("localhost", port);
    }

    @After
    public void stopServer() throws InterruptedException {
        server.shutdown();
        client.shutdown();
    }

    /**
     * Just here to show that things work without the compression
     */
    @Test
    public void getUnzippedContent() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, "/test");
        testRequest(client, request);
    }

    /**
     * The actual test - fails with a IllegalReferenceCountException
     */
    @Test
    public void getZippedContent() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, "/test");
        request.withHeader("Accept-Encoding", "gzip, deflate");
        testRequest(client, request);
    }

    /**
     * Test a request by sending it to the server and then asserting the answer we get back is correct.
     */
    private static void testRequest(HttpClient<ByteBuf, ByteBuf> client, HttpClientRequest<ByteBuf> request) {
        String message = client.submit(request)
                               .flatMap(getContent)
                               .map(toString)
                               .toBlocking()
                               .single();
        Assert.assertEquals(MESSAGE, message);
    }

    /**
     * Ignore the headers etc. just get the response content.
     */
    private static final Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>> getContent = new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
        @Override
        public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
            return response.getContent();
        }
    };

    /**
     * Converts a ByteBuf to a string - assumes UTF-8
     */
    private static final Func1<ByteBuf, String> toString = new Func1<ByteBuf, String>() {
        @Override
        public String call(ByteBuf byteBuf) {
            return byteBuf.toString(StandardCharsets.UTF_8);
        }
    };

    /**
     * Create a dumb server that just responds to any request with the same "Hello World!" response.
     * If there's an "Accept-Encoding" header with gzip the response will be zipped before its returned.
     */
    private static HttpServer<ByteBuf, ByteBuf> createServer() {
        return RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                String acceptEncoding = request.getHeaders().get("Accept-Encoding");
                if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                    response.getHeaders().add("Content-Encoding", "gzip");
                    byte[] zMessage = zipMessage(MESSAGE);
                    return response.writeBytesAndFlush(zMessage);
                } else {
                    return response.writeStringAndFlush(MESSAGE);
                }
            }
        }).pipelineConfigurator(new HttpServerPipelineConfigurator<ByteBuf, ByteBuf>()).build();
    }

    /**
     * Create a simple client with the a content decompressor
     */
    private static HttpClient<ByteBuf, ByteBuf> createClient(String host, int port) {
        HttpClientBuilder<ByteBuf, ByteBuf> builder = RxNetty.newHttpClientBuilder(host, port);

        builder.pipelineConfigurator(
                new PipelineConfiguratorComposite<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>>(
                        new HttpClientPipelineConfigurator<ByteBuf, ByteBuf>(),
                        gzipPipelineConfigurator)
        );

        return builder.build();
    }

    /**
     * Configurator so that we can support setting the "Accept-Encoding: gzip, deflate" header.
     */
    private static final PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> gzipPipelineConfigurator = new PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>>() {
        @Override
        public void configureNewPipeline(ChannelPipeline pipeline) {
            ChannelHandler handlers = new HttpContentDecompressor();
            pipeline.addLast(handlers);
        }
    };

    /**
     * Returns a byte array with the message gzipped.
     */
    private static byte[] zipMessage(String message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzos = new GZIPOutputStream(out);
            try {
                gzos.write(message.getBytes(StandardCharsets.UTF_8));
            } finally {
                gzos.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }
}