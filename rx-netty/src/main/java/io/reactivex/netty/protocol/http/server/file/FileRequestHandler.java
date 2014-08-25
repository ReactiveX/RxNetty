package io.reactivex.netty.protocol.http.server.file;

import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpError;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rx.Observable;
import rx.functions.Action0;

/**
 * Base implementation for serving local files.  Resolving the request URI to
 * a local file URI is deferred to the subclass.
 * 
 * @author elandau
 *
 */
public abstract class FileRequestHandler extends AbstractFileRequestHandler {
    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        // We don't support GET.  
        if (!request.getHttpMethod().equals(HttpMethod.GET)) {
            return Observable.error(new HttpError(HttpResponseStatus.NOT_FOUND));
        }
        
        RandomAccessFile raf = null;
        
        try {
            String sanitizedUri = sanitizeUri(request.getUri());
            if (sanitizedUri == null) {
                return Observable.error(new HttpError(HttpResponseStatus.FORBIDDEN));
            }
            
            URI uri = resolveUri(sanitizedUri);
            if (uri == null) {
                return Observable.error(new HttpError(HttpResponseStatus.NOT_FOUND));
            }
            
            File file = new File(uri);
            if (file.isHidden() || !file.exists()) {
                return Observable.error(new HttpError(HttpResponseStatus.NOT_FOUND));
            }

            if (file.isDirectory()) {
                return Observable.error(new HttpError(HttpResponseStatus.FORBIDDEN));
            }
            
            if (!file.isFile()) {
                return Observable.error(new HttpError(HttpResponseStatus.FORBIDDEN));
            }

            raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();

            // Cache Validation
            String ifModifiedSince = request.getHeaders().get(HttpHeaders.Names.IF_MODIFIED_SINCE);
            if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
                Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

                // Only compare up to the second because the datetime format we send to the client
                // does not have milliseconds
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    response.setStatus(HttpResponseStatus.NOT_MODIFIED);
                    setDateHeader(response);
                    return response.close().finallyDo(closeFileAction(raf));
                }
            }
            
            response.setStatus(HttpResponseStatus.OK);
            response.getHeaders().setContentLength(fileLength);
            setContentTypeHeader(response, file);
            setDateAndCacheHeaders(response, file);
            response.writeFileRegion(new DefaultFileRegion(raf.getChannel(), 0, fileLength));
            
            // TODO: Handle keep alive headers
            return response.close().finallyDo(closeFileAction(raf));
        }
        catch (Exception e) {
            return Observable.error(e);
        }
    }

    protected abstract URI resolveUri(String path);
    
    public Action0 closeFileAction(final RandomAccessFile file) {
        return new Action0() {
            @Override
            public void call() {
                try {
                    file.close();
                } catch (IOException e) {
                }
            }
        };
    }
}
