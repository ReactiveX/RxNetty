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
package io.reactivex.netty.protocol.http.server.file;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.ErrorResponseGenerator;
import io.reactivex.netty.protocol.http.server.HttpError;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.functions.Func1;

/**
 * Custom error mapper to render 404 errors when serving files.  All other errors
 * are defered to the default handler
 * 
 * @author elandau
 *
 */
public class FileErrorResponseMapper implements Func1<Throwable, ErrorResponseGenerator<ByteBuf>> {

    public static final String  _404_HTML_TEMPLATE = 
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>Http Error 404</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h1>File not found.</h1>\n" +
            "</body>\n" +
            "</html>";
    
    private static class ConstantErrorResponseGenerator<O> implements ErrorResponseGenerator<O> {
        public final String template;
        
        public ConstantErrorResponseGenerator(String template) {
            this.template = template;
        }
        
        @Override
        public void updateResponse(HttpServerResponse<O> response, Throwable t) {
            HttpError error = (HttpError)t;
            response.setStatus(error.getStatus());
            response.getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
            response.writeString(template);
        }
    }
    
    @Override
    public ErrorResponseGenerator<ByteBuf> call(Throwable t1) {
        if (t1 instanceof HttpError) {
            HttpError error = (HttpError)t1;
            if (error.getStatus().equals(HttpResponseStatus.NOT_FOUND)) {
                return new ConstantErrorResponseGenerator<ByteBuf>(_404_HTML_TEMPLATE);
            }
        }
        return null;
    }
}
