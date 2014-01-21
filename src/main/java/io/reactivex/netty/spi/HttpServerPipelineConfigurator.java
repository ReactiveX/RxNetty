package io.reactivex.netty.spi;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * An implementation of {@link NettyPipelineConfigurator} to configure the pipeline for an HTTP server. <br/>
 * This will configure the pipeline that will produce the following events:
 *
 * <h2>Request</h2>
 * <ul>
 <li>One {@link HttpRequest} object.</li>
 <li>Zero or more {@link HttpContent} object</li>
 <li>One {@link LastHttpContent} object.</li>
 </ul>
 *
 * <h2>Response</h2>
 * <ul>
 <li>One {@link HttpResponse} object.</li>
 <li>Zero or more {@link HttpContent} object</li>
 <li>One {@link LastHttpContent} object.</li>
 </ul>
 *
 * <h2>Configuration parameters</h2>
 * This class provides all the configuration options provided by {@link HttpRequestDecoder}, with the following defaults:<br/>
 *
 * <table border="1">
 * <tr>
 * <th>Name</th><th>Default</th>
 * </tr>
 * <tr>
 * <td>{@code maxInitialLineLength}</td>
 * <td>{@link #MAX_INITIAL_LINE_LENGTH_DEFAULT}</td>
 * </tr>
 * <tr>
 * <td>{@code maxHeaderSize}</td>
 * <td>{@link #MAX_HEADER_SIZE_DEFAULT}</td>
 * </tr>
 * <tr>
 * <td>{@code maxChunkSize}</td>
 * <td>{@link #MAX_CHUNK_SIZE_DEFAULT}</td>
 * </tr>
 * <tr>
 * <td>{@code validateHeaders}</td>
 * <td>{@link #VALIDATE_HEADERS_DEFAULT}</td>
 * </tr>
 * </table>
 *
 * @see {@link HttpRequestDecoder}
 * @see {@link HttpResponseEncoder}
 *
 * @author Nitesh Kant
 */
public class HttpServerPipelineConfigurator extends HttpPipelineConfigurator {

    public static final String HTTP_REQUEST_DECODER_HANDLER_NAME = "http-request-decoder";
    public static final String HTTP_RESPONSE_ENCODER_HANDLER_NAME = "http-response-encoder";

    public HttpServerPipelineConfigurator() {
        this(MAX_INITIAL_LINE_LENGTH_DEFAULT, MAX_CHUNK_SIZE_DEFAULT, MAX_HEADER_SIZE_DEFAULT);
    }

    public HttpServerPipelineConfigurator(int maxInitialLineLength, int maxChunkSize, int maxHeaderSize) {
        super(maxInitialLineLength, maxChunkSize, maxHeaderSize, VALIDATE_HEADERS_DEFAULT);
    }

    public HttpServerPipelineConfigurator(int maxInitialLineLength, int maxChunkSize, int maxHeaderSize,
                                          boolean validateHeaders) {
        super(maxInitialLineLength, maxChunkSize, maxHeaderSize, validateHeaders);
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(HTTP_REQUEST_DECODER_HANDLER_NAME, new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize,
                                                                                   maxChunkSize, validateHeaders));
        pipeline.addLast(HTTP_RESPONSE_ENCODER_HANDLER_NAME, new HttpResponseEncoder());
    }
}
