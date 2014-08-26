package io.reactivex.netty.protocol.http.server.file;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileRequestHandler that reads files from the class path
 * 
 * @author elandau
 *
 */
public class ClassPathFileRequestHandler extends FileRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClassPathFileRequestHandler.class);
    
    private final String prefix;
    
    public ClassPathFileRequestHandler(String prefix) {
        this.prefix = prefix;
        
        // Remove any trailing '/'s
        while (prefix.endsWith(File.separator))
            prefix = prefix.substring(0, prefix.length()-1);

    }
     
    @Override
    protected URI resolveUri(String path) {
        String resourcePath = prefix + path;
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
            if (url == null) {
                logger.debug("Resource '{}' not found ", resourcePath);
                return null;
            }
            return url.toURI();
        } catch (URISyntaxException e) {
            logger.debug("Error resovlving uri for '{}'", resourcePath);
            return null;
        }
    }
}
