package io.reactivex.netty.protocol.http;

import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Nitesh Kant
 */
public abstract class HttpPipelineConfigurator<R, W>
        implements PipelineConfigurator<R, W> {

    public static final int MAX_INITIAL_LINE_LENGTH_DEFAULT = 4096;
    public static final int MAX_HEADER_SIZE_DEFAULT = 8192;
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;
    public static final boolean VALIDATE_HEADERS_DEFAULT = true;
    protected final int maxInitialLineLength;
    protected final int maxHeaderSize;
    protected final int maxChunkSize;
    protected final boolean validateHeaders;

    protected HttpPipelineConfigurator(int maxInitialLineLength, int maxChunkSize, int maxHeaderSize,
                                       boolean validateHeaders) {
        this.maxInitialLineLength = maxInitialLineLength;
        this.validateHeaders = validateHeaders;
        this.maxChunkSize = maxChunkSize;
        this.maxHeaderSize = maxHeaderSize;
    }
}
