package io.reactivex.netty.protocol.http.server.file;

import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class LocalDirectoryURIResolver implements URIResolver {
    private final String prefix;
    
    public LocalDirectoryURIResolver() {
        this(SystemPropertyUtil.get("user.dir") + File.separator);
    }
    
    public LocalDirectoryURIResolver(String prefix) {
        this.prefix = prefix;
    }
     
    @Override
    public URI getUri(String path) {
        try {
            return new URI("file:///" + prefix + path);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
