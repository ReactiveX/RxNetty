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
package io.reactivex.netty.protocol.http.server;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.AbstractHttpConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} to configure the pipeline for an HTTP server. <br/>
 * This will configure the pipeline that will produce/consume the following events:
 *
 * <h2>Produce</h2>
 * <ul>
 <li>One {@link HttpRequest} object.</li>
 <li>Zero or more {@link HttpContent} object</li>
 <li>One {@link LastHttpContent} object.</li>
 </ul>
 *
 * <h2>Consume</h2>
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
 * @see HttpRequestDecoder
 * @see HttpResponseEncoder
 *
 * @author Nitesh Kant
 */
public class HttpServerPipelineConfigurator<I, O> extends AbstractHttpConfigurator
        implements PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> {

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
