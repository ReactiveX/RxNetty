/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http.client;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.AbstractHttpConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} to configure the pipeline for an HTTP client. <br/>
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
 * @see HttpClientCodec
 *
 * @author Nitesh Kant
 */
public class HttpClientPipelineConfigurator<I, O> extends AbstractHttpConfigurator
        implements PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> {

    public static final String REQUEST_RESPONSE_CONVERTER_HANDLER_NAME = "request-response-converter";
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
