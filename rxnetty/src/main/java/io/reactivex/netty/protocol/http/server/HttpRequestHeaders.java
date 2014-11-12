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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class HttpRequestHeaders {

    private final HttpRequest nettyRequest;
    private final HttpHeaders nettyHeaders;

    HttpRequestHeaders(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        nettyHeaders = this.nettyRequest.headers();
    }

    public boolean contains(CharSequence name) {
        return nettyHeaders.contains(name);
    }

    public boolean contains(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return nettyHeaders.contains(name, value, ignoreCaseValue);
    }

    public boolean contains(String name) {
        return nettyHeaders.contains(name);
    }

    public boolean contains(String name, String value, boolean ignoreCaseValue) {
        return nettyHeaders.contains(name, value, ignoreCaseValue);
    }

    public void encodeAscii(CharSequence seq, ByteBuf buf) {
        HttpHeaders.encodeAscii(seq, buf);
    }

    public List<Map.Entry<String, String>> entries() {
        return nettyHeaders.entries();
    }

    public boolean equalsIgnoreCase(CharSequence name1, CharSequence name2) {
        return HttpHeaders.equalsIgnoreCase(name1, name2);
    }

    public String get(CharSequence name) {
        return nettyHeaders.get(name);
    }

    public String get(String name) {
        return nettyHeaders.get(name);
    }

    public List<String> getAll(CharSequence name) {
        return nettyHeaders.getAll(name);
    }

    public List<String> getAll(String name) {
        return nettyHeaders.getAll(name);
    }

    public long getContentLength() {
        return HttpHeaders.getContentLength(nettyRequest);
    }

    public long getContentLength(long defaultValue) {
        return HttpHeaders.getContentLength(nettyRequest, defaultValue);
    }

    public Date getDate() throws ParseException {
        return HttpHeaders.getDate(nettyRequest);
    }

    public Date getDate(Date defaultValue) {
        return HttpHeaders.getDate(nettyRequest, defaultValue);
    }

    public Date getDateHeader(CharSequence name) throws ParseException {
        return HttpHeaders.getDateHeader(nettyRequest, name);
    }

    public Date getDateHeader(CharSequence name,
                                     Date defaultValue) {
        return HttpHeaders.getDateHeader(nettyRequest, name, defaultValue);
    }

    public Date getDateHeader(String name) throws ParseException {
        return HttpHeaders.getDateHeader(nettyRequest, name);
    }

    public Date getDateHeader(String name,
                                     Date defaultValue) {
        return HttpHeaders.getDateHeader(nettyRequest, name, defaultValue);
    }

    public String getHeader(CharSequence name) {
        return HttpHeaders.getHeader(nettyRequest, name);
    }

    public String getHeader(CharSequence name,
                                   String defaultValue) {
        return HttpHeaders.getHeader(nettyRequest, name, defaultValue);
    }

    public String getHeader(String name) {
        return HttpHeaders.getHeader(nettyRequest, name);
    }

    public String getHeader(String name,
                                   String defaultValue) {
        return HttpHeaders.getHeader(nettyRequest, name, defaultValue);
    }

    public String getHost() {
        return HttpHeaders.getHost(nettyRequest);
    }

    public String getHost(String defaultValue) {
        return HttpHeaders.getHost(nettyRequest, defaultValue);
    }

    public int getIntHeader(CharSequence name) {
        return HttpHeaders.getIntHeader(nettyRequest, name);
    }

    public int getIntHeader(CharSequence name, int defaultValue) {
        return HttpHeaders.getIntHeader(nettyRequest, name, defaultValue);
    }

    public int getIntHeader(String name) {
        return HttpHeaders.getIntHeader(nettyRequest, name);
    }

    public int getIntHeader(String name, int defaultValue) {
        return HttpHeaders.getIntHeader(nettyRequest, name, defaultValue);
    }

    public boolean is100ContinueExpected() {
        return HttpHeaders.is100ContinueExpected(nettyRequest);
    }

    public boolean isContentLengthSet() {
        return HttpHeaders.isContentLengthSet(nettyRequest);
    }

    public boolean hasContent() {
        if (isContentLengthSet()) {
            return getContentLength() > 0;
        } else {
            return isTransferEncodingChunked();
        }
    }

    public boolean isEmpty() {
        return nettyHeaders.isEmpty();
    }

    public boolean isKeepAlive() {
        return HttpHeaders.isKeepAlive(nettyRequest);
    }

    public boolean isTransferEncodingChunked() {
        return HttpHeaders.isTransferEncodingChunked(nettyRequest);
    }

    public Set<String> names() {
        return nettyHeaders.names();
    }
}
