package io.reactivex.netty.protocol.http.server.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import rx.Observable;

import com.google.common.net.HttpHeaders;

public class FileResponses {
    public static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    /**
     * Send an error response with text describing the error
     * 
     * @param response
     * @param status
     * @return 
     */
    public static Observable<Void> sendError(HttpServerResponse<ByteBuf> response, HttpResponseStatus status) {
        response.setStatus(status);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        return response.writeAndFlush(Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
    }
    
    /**
     * Sets the Date header for the HTTP response
     *
     * @param response
     *            HTTP response
     */
    public static void setDateHeader(HttpServerResponse<ByteBuf> response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.getHeaders().set(HttpHeaders.DATE, dateFormatter.format(time.getTime()));
    }
    
    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    public static void setDateAndCacheHeaders(HttpServerResponse<ByteBuf> response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.getHeaders().set(HttpHeaders.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.getHeaders().set(HttpHeaders.EXPIRES, dateFormatter.format(time.getTime()));
        response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.getHeaders().set(HttpHeaders.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param file
     *            file to extract content type
     */
    public static void setContentTypeHeader(HttpServerResponse<ByteBuf> response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }
}
