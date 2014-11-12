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
import io.netty.handler.codec.http.HttpResponse;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class HttpResponseHeaders {

    private final HttpResponse nettyResponse;
    private final HttpHeaders nettyHeaders;

    public HttpResponseHeaders(HttpResponse nettyResponse) {
        this.nettyResponse = nettyResponse;
        nettyHeaders = this.nettyResponse.headers();
    }

    public HttpHeaders add(HttpHeaders headers) {
        return nettyHeaders.add(headers);
    }

    public HttpHeaders add(CharSequence name, Object value) {
        return nettyHeaders.add(name, value);
    }

    public HttpHeaders add(CharSequence name, Iterable<?> values) {
        return nettyHeaders.add(name, values);
    }

    public HttpHeaders add(String name, Object value) {
        return nettyHeaders.add(name, value);
    }

    public HttpHeaders add(String name, Iterable<?> values) {
        return nettyHeaders.add(name, values);
    }

    public void addDateHeader(CharSequence name, Date value) {
        HttpHeaders.addDateHeader(nettyResponse, name, value);
    }

    public void addDateHeader(String name, Date value) {
        HttpHeaders.addDateHeader(nettyResponse, name, value);
    }

    public void addHeader(CharSequence name, Object value) {
        HttpHeaders.addHeader(nettyResponse, name, value);
    }

    public void addHeader(String name, Object value) {
        HttpHeaders.addHeader(nettyResponse, name, value);
    }

    public void addIntHeader(CharSequence name, int value) {
        HttpHeaders.addIntHeader(nettyResponse, name, value);
    }

    public void addIntHeader(String name, int value) {
        HttpHeaders.addIntHeader(nettyResponse, name, value);
    }

    public HttpHeaders clear() {
        return nettyHeaders.clear();
    }

    public void clearHeaders() {
        HttpHeaders.clearHeaders(nettyResponse);
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
        return HttpHeaders.getContentLength(nettyResponse);
    }

    public long getContentLength(long defaultValue) {
        return HttpHeaders.getContentLength(nettyResponse, defaultValue);
    }

    public Date getDate() throws ParseException {
        return HttpHeaders.getDate(nettyResponse);
    }

    public Date getDate(Date defaultValue) {
        return HttpHeaders.getDate(nettyResponse, defaultValue);
    }

    public Date getDateHeader(CharSequence name) throws ParseException {
        return HttpHeaders.getDateHeader(nettyResponse, name);
    }

    public Date getDateHeader(CharSequence name,
                                     Date defaultValue) {
        return HttpHeaders.getDateHeader(nettyResponse, name, defaultValue);
    }

    public Date getDateHeader(String name) throws ParseException {
        return HttpHeaders.getDateHeader(nettyResponse, name);
    }

    public Date getDateHeader(String name,
                                     Date defaultValue) {
        return HttpHeaders.getDateHeader(nettyResponse, name, defaultValue);
    }

    public String getHeader(CharSequence name) {
        return HttpHeaders.getHeader(nettyResponse, name);
    }

    public String getHeader(CharSequence name,
                                   String defaultValue) {
        return HttpHeaders.getHeader(nettyResponse, name, defaultValue);
    }

    public String getHeader(String name) {
        return HttpHeaders.getHeader(nettyResponse, name);
    }

    public String getHeader(String name,
                                   String defaultValue) {
        return HttpHeaders.getHeader(nettyResponse, name, defaultValue);
    }

    public String getHost() {
        return HttpHeaders.getHost(nettyResponse);
    }

    public String getHost(String defaultValue) {
        return HttpHeaders.getHost(nettyResponse, defaultValue);
    }

    public int getIntHeader(CharSequence name) {
        return HttpHeaders.getIntHeader(nettyResponse, name);
    }

    public int getIntHeader(CharSequence name, int defaultValue) {
        return HttpHeaders.getIntHeader(nettyResponse, name, defaultValue);
    }

    public int getIntHeader(String name) {
        return HttpHeaders.getIntHeader(nettyResponse, name);
    }

    public int getIntHeader(String name, int defaultValue) {
        return HttpHeaders.getIntHeader(nettyResponse, name, defaultValue);
    }

    public boolean is100ContinueExpected() {
        return HttpHeaders.is100ContinueExpected(nettyResponse);
    }

    public boolean isContentLengthSet() {
        return HttpHeaders.isContentLengthSet(nettyResponse);
    }

    public boolean isEmpty() {
        return nettyHeaders.isEmpty();
    }

    public boolean isKeepAlive() {
        return HttpHeaders.isKeepAlive(nettyResponse);
    }

    public boolean isTransferEncodingChunked() {
        return HttpHeaders.isTransferEncodingChunked(nettyResponse);
    }

    public Set<String> names() {
        return nettyHeaders.names();
    }

    public CharSequence newEntity(String name) {
        return HttpHeaders.newEntity(name);
    }

    public HttpHeaders remove(CharSequence name) {
        return nettyHeaders.remove(name);
    }

    public HttpHeaders remove(String name) {
        return nettyHeaders.remove(name);
    }

    public void removeHeader(CharSequence name) {
        HttpHeaders.removeHeader(nettyResponse, name);
    }

    public void removeHeader(String name) {
        HttpHeaders.removeHeader(nettyResponse, name);
    }

    public void removeTransferEncodingChunked() {
        HttpHeaders.removeTransferEncodingChunked(nettyResponse);
    }

    public HttpHeaders set(HttpHeaders headers) {
        return nettyHeaders.set(headers);
    }

    public HttpHeaders set(CharSequence name, Object value) {
        return nettyHeaders.set(name, value);
    }

    public HttpHeaders set(CharSequence name, Iterable<?> values) {
        return nettyHeaders.set(name, values);
    }

    public HttpHeaders set(String name, Object value) {
        return nettyHeaders.set(name, value);
    }

    public HttpHeaders set(String name, Iterable<?> values) {
        return nettyHeaders.set(name, values);
    }

    public void set100ContinueExpected() {
        HttpHeaders.set100ContinueExpected(nettyResponse);
    }

    public void set100ContinueExpected(boolean set) {
        HttpHeaders.set100ContinueExpected(nettyResponse, set);
    }

    public void setContentLength(long length) {
        HttpHeaders.setContentLength(nettyResponse, length);
    }

    public void setDate(Date value) {
        HttpHeaders.setDate(nettyResponse, value);
    }

    public void setDateHeader(CharSequence name,
                                     Date value) {
        HttpHeaders.setDateHeader(nettyResponse, name, value);
    }

    public void setDateHeader(CharSequence name,
                                     Iterable<Date> values) {
        HttpHeaders.setDateHeader(nettyResponse, name, values);
    }

    public void setDateHeader(String name, Date value) {
        HttpHeaders.setDateHeader(nettyResponse, name, value);
    }

    public void setDateHeader(String name,
                                     Iterable<Date> values) {
        HttpHeaders.setDateHeader(nettyResponse, name, values);
    }

    public void setHeader(CharSequence name,
                                 Object value) {
        HttpHeaders.setHeader(nettyResponse, name, value);
    }

    public void setHeader(CharSequence name,
                                 Iterable<?> values) {
        HttpHeaders.setHeader(nettyResponse, name, values);
    }

    public void setHeader(String name, Object value) {
        HttpHeaders.setHeader(nettyResponse, name, value);
    }

    public void setHeader(String name,
                                 Iterable<?> values) {
        HttpHeaders.setHeader(nettyResponse, name, values);
    }

    public void setHost(CharSequence value) {
        HttpHeaders.setHost(nettyResponse, value);
    }

    public void setHost(String value) {
        HttpHeaders.setHost(nettyResponse, value);
    }

    public void setIntHeader(CharSequence name, int value) {
        HttpHeaders.setIntHeader(nettyResponse, name, value);
    }

    public void setIntHeader(CharSequence name,
                                    Iterable<Integer> values) {
        HttpHeaders.setIntHeader(nettyResponse, name, values);
    }

    public void setIntHeader(String name, int value) {
        HttpHeaders.setIntHeader(nettyResponse, name, value);
    }

    public void setIntHeader(String name,
                                    Iterable<Integer> values) {
        HttpHeaders.setIntHeader(nettyResponse, name, values);
    }

    public void setKeepAlive(boolean keepAlive) {
        HttpHeaders.setKeepAlive(nettyResponse, keepAlive);
    }

    public void setTransferEncodingChunked() {
        HttpHeaders.setTransferEncodingChunked(nettyResponse);
    }
}
