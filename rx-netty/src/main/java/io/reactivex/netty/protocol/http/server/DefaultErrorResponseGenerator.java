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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.RxNetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.PrintStream;
import java.nio.charset.Charset;

/**
* @author Nitesh Kant
*/
class DefaultErrorResponseGenerator<O> implements ErrorResponseGenerator<O> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorResponseGenerator.class);

    public static final String STACKTRACE_TEMPLATE_VARIABLE = "${stacktrace}";
    private static final String ERROR_HTML_TEMPLATE = "<!DOCTYPE html>\n" +
                                                      "<html>\n" +
                                                      "<head>\n" +
                                                      "    <title>RxNetty Error Page.</title>\n" +
                                                      "</head>\n" +
                                                      "<body>\n" +
                                                      "    <h1>Unexpected error occured in the server.</h1>\n" +
                                                      "    <h3>Error</h3>\n" +
                                                      "    <PRE>  " + STACKTRACE_TEMPLATE_VARIABLE + " </PRE>\n" +
                                                      "</body>\n" +
                                                      "</html>";

    @Override
    public void updateResponse(HttpServerResponse<O> response, Throwable error) {
        if (error instanceof HttpError) {
            response.setStatus(((HttpError)error).getStatus());
        }
        else {
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        response.getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
        ByteBuf buffer = response.getChannel().alloc().buffer(1024);// 1KB initial length.
        PrintStream printStream = null;
        try {
            printStream = new PrintStream(new ByteBufOutputStream(buffer));
            error.printStackTrace(printStream);
            String errorPage = ERROR_HTML_TEMPLATE.replace(STACKTRACE_TEMPLATE_VARIABLE,
                                                           buffer.toString(Charset.defaultCharset()));
            response.writeString(errorPage);
        } finally {
            ReferenceCountUtil.release(buffer);
            if (null != printStream) {
                try {
                    printStream.flush();
                    printStream.close();
                } catch (Exception e) {
                    logger.error("Error closing stream for generating error response stacktrace. This is harmless.", e);
                }
            }
        }
    }

    public static void main(String[] args) {
        RxNetty.createHttpServer(8888, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                throw new NullPointerException("doomsday");
            }
        }).startAndWait();
    }
}
