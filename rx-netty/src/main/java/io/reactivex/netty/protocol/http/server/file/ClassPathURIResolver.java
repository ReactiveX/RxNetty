package io.reactivex.netty.protocol.http.server.file;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Resolve the URL for a request URI from the classpath
 * 
 * @author elandau
 *
 */
public class ClassPathURIResolver implements URIResolver {
    private static final String DEFAULT_PATH_PREFIX = "WEB-INF";
    
    private final String prefix;
    
    public ClassPathURIResolver() {
        this(DEFAULT_PATH_PREFIX);
    }
    
    public ClassPathURIResolver(String prefix) {
        this.prefix = prefix;
    }
     
    @Override
    public URI getUri(String path) {
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
