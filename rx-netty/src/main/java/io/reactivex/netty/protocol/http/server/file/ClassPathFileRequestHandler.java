package io.reactivex.netty.protocol.http.server.file;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * FileRequestHandler that reads files from the class path
 * 
 * @author elandau
 *
 */
public class ClassPathFileRequestHandler extends FileRequestHandler {
    
    private final String prefix;
    
    public ClassPathFileRequestHandler(String prefix) {
        this.prefix = prefix;
        
        // Remove any trailing '/'s
        while (prefix.endsWith(File.separator))
            prefix = prefix.substring(0, prefix.length()-1);

    }
     
    @Override
    protected URI resolveUri(String path) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(prefix + path);
            if (url == null)
                return null;
            return url.toURI();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
