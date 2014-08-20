package io.reactivex.netty.protocol.http.server.file;

import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rx.Observable;

/**
 * Base implementation for serving local files.  Resolving the request URI to
 * a local file URI is deferred to the subclass.
 * 
 * @author elandau
 *
 */
public class FileRequestHandler implements RequestHandler<ByteBuf, ByteBuf> {
    private final URIResolver resolver;
    
    public FileRequestHandler(URIResolver resolver) {
        this.resolver = resolver;
    }
    
    @Override
    public Observable<Void> handle(
            HttpServerRequest<ByteBuf> request,
            HttpServerResponse<ByteBuf> response) {
        
        if (!request.getHttpMethod().equals(HttpMethod.GET)) {
            return Observable.empty();
        }
        
        try {
            URI uri = resolver.getUri(request.getUri());
            if (uri == null) {
                return FileResponses.sendError(response, HttpResponseStatus.NOT_FOUND);
            }
            
            File file = new File(uri);
            if (file.isHidden() || !file.exists()) {
                return FileResponses.sendError(response, HttpResponseStatus.NOT_FOUND);
            }

            if (file.isDirectory()) {
                // File listing not allowed
                return FileResponses.sendError(response, HttpResponseStatus.FORBIDDEN);
            }
            
            if (!file.isFile()) {
                return FileResponses.sendError(response, HttpResponseStatus.FORBIDDEN);
            }

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();

            // Cache Validation
            String ifModifiedSince = request.getHeaders().get(HttpHeaders.Names.IF_MODIFIED_SINCE);
            if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat(FileResponses.HTTP_DATE_FORMAT, Locale.US);
                Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

                // Only compare up to the second because the datetime format we send to the client
                // does not have milliseconds
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    response.setStatus(HttpResponseStatus.NOT_MODIFIED);
                    FileResponses.setDateHeader(response);
                    return Observable.empty();
                }
            }
            
            response.setStatus(HttpResponseStatus.OK);
            response.getHeaders().setContentLength(fileLength);
            FileResponses.setContentTypeHeader(response, file);
            FileResponses.setDateAndCacheHeaders(response, file);
            response.writeFileRegion(new DefaultFileRegion(raf.getChannel(), 0, fileLength));
            return response.close();
//            if (HttpHeaderUtil.isKeepAlive(request)) {
//                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
//            }
        }
        catch (Exception e) {
            return FileResponses.sendError(response, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
