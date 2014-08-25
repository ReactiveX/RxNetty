package io.reactivex.netty.protocol.http.server.file;

import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

/**
 * FileRequestHandler that reads files from the file system
 * 
 * @author elandau
 */
public class LocalDirectoryRequestHandler extends FileRequestHandler {
    private final String prefix;
    
    public LocalDirectoryRequestHandler() {
        this(SystemPropertyUtil.get("user.dir") + File.separator);
    }
    
    public LocalDirectoryRequestHandler(String prefix) {
        this.prefix = prefix;
    }
     
    @Override
    protected URI resolveUri(String path) {
        try {
            URI uri = new URI("file:///" + prefix + path);
            if (Files.notExists(Paths.get(uri), LinkOption.NOFOLLOW_LINKS)) {
                return null;
            }
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
