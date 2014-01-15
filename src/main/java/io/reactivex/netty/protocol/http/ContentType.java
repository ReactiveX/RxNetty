/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http;

import java.nio.charset.Charset;

/**
 * This class represents an HTTP content type. It does not support arbitrary parameter, though.
 * Instead, it assumes that a content type has at most one parameter: charset. Feel free to enhance it.
 */
public class ContentType {
    public static final String HEADER_NAME = "Content-Type";
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    public static final String DEFAULT_MEDIA_TYPE = "text/plain";
    public static final String CHARSET_PREFIX = "charset=";

    private final String mediaType;
    private final Charset charset;

    public ContentType(String mediaType, Charset charset) {

        this.mediaType = mediaType;
        this.charset = charset;
    }

    public static final ContentType DEFAULT_CONTENT_TYPE = new ContentType(DEFAULT_MEDIA_TYPE, DEFAULT_CHARSET);

    public String getMediaType() {
        return mediaType;
    }

    public Charset getCharset() {
        return charset;
    }

    public boolean isSSE() {
        return "text/event-stream".equals(getMediaType());
    }

    // Content-Type: text/html; charset=ISO-8859-4
    public static ContentType fromHeader(String headerValue) {
        if (headerValue == null || headerValue.trim().length() == 0) {
            return DEFAULT_CONTENT_TYPE;
        }

        String[] pair = headerValue.split("\\s*;\\s*");
        String mediaType = pair[0].trim();

        if (pair.length < 2) {
            return new ContentType(mediaType, DEFAULT_CHARSET);
        }

        String charset = pair[1].trim();
        if (!charset.startsWith(CHARSET_PREFIX)) {
            return new ContentType(mediaType, DEFAULT_CHARSET);
        }

        String charsetName = charset.substring(CHARSET_PREFIX.length());
        if (charsetName == null) {
            return new ContentType(mediaType, DEFAULT_CHARSET);
        }

        try {
            // No need to use Charse.isSupported() because this method will throw
            // exceptions for illegal charset name anyway.
            return new ContentType(mediaType, Charset.forName(charsetName));
        } catch (Exception e) {
            return new ContentType(mediaType, DEFAULT_CHARSET);
        }

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ContentType{");
        sb.append("charset=").append(charset);
        sb.append(", mediaType='").append(mediaType).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ContentType that = (ContentType) o;

        if (charset != null ? !charset.equals(that.charset) : that.charset != null)
            return false;
        if (mediaType != null ? !mediaType.equals(that.mediaType) : that.mediaType != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mediaType != null ? mediaType.hashCode() : 0;
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        return result;
    }

    public static void main(String[] args) {
        String[] headers = {
                "  text/event-stream;charset=UTF-8",
                "text/html; charset=ISO-8859-4",
                "text/html; charset=ISO-88",
                "text/html; charset=",
                "text/html; charset=",
                "  text/html; charse",
                "texT/html; ",
                "text/hTml",
                "text/h",
                "",
                null,
        };

        for (String header : headers) {
            ContentType contentType = ContentType.fromHeader(header);
            System.out.println(contentType);
        }
    }
}
