package io.reactivex.netty.http;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * An implementation of {@link NettyPipelineConfigurator} to configure the pipeline for an HTTP client. <br/>
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
 * This class provides all the configuration options provided by {@link HttpClientCodec}, with the following defaults:<br/>
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
 * <tr>
 * <td>{@code failOnMissingResponse}</td>
 * <td>{@link #FAIL_ON_MISSING_RESPONSE_DEFAULT}</td>
 * </tr>
 * </table>
 *
 * @see {@link HttpClientCodec}
 *
 * @author Nitesh Kant
 */
public class HttpClientPipelineConfigurator extends HttpPipelineConfigurator {

    public static final String HTTP_CODEC_HANDLER_NAME = "http-client-codec";

    public static final boolean FAIL_ON_MISSING_RESPONSE_DEFAULT = true;

    private final boolean failOnMissingResponse;

    public HttpClientPipelineConfigurator() {
        this(MAX_INITIAL_LINE_LENGTH_DEFAULT, MAX_HEADER_SIZE_DEFAULT, MAX_CHUNK_SIZE_DEFAULT, VALIDATE_HEADERS_DEFAULT,
             FAIL_ON_MISSING_RESPONSE_DEFAULT);
    }

    public HttpClientPipelineConfigurator(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
                                          boolean validateHeaders) {
        this(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders, FAIL_ON_MISSING_RESPONSE_DEFAULT);
    }

    public HttpClientPipelineConfigurator(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        this(maxInitialLineLength, maxHeaderSize, maxChunkSize, VALIDATE_HEADERS_DEFAULT,
             FAIL_ON_MISSING_RESPONSE_DEFAULT);
    }

    public HttpClientPipelineConfigurator(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
                                          boolean validateHeaders, boolean failOnMissingResponse) {
        super(maxInitialLineLength, maxChunkSize, maxHeaderSize, validateHeaders);
        this.failOnMissingResponse = failOnMissingResponse;
    }


    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(HTTP_CODEC_HANDLER_NAME,
                         new HttpClientCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, failOnMissingResponse,
                                             validateHeaders));
    }
}
