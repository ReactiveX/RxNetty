package io.reactivex.netty.protocol.http.server.file;

import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileRequestHandler that reads files from the file system
 * 
 * @author elandau
 */
public class LocalDirectoryRequestHandler extends FileRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(LocalDirectoryRequestHandler.class);
    
    private final String prefix;
    
    public LocalDirectoryRequestHandler() {
        this(SystemPropertyUtil.get("user.dir") + File.separator);
    }
    
    public LocalDirectoryRequestHandler(String prefix) {
        this.prefix = "file:///" + prefix;
    }
     
    @Override
    protected URI resolveUri(String path) {
        String filename = prefix + path;
        try {
            URI uri = new URI(filename);
            File file = new File(uri);
            if (!file.exists()) {
                logger.debug("File '{}' not found", filename);
                return null;
            }
            return uri;
        } catch (URISyntaxException e) {
            logger.debug("Error resovlving uri for '{}'", filename);
            return null;
        }
    }
}
