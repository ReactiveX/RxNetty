package io.reactivex.netty.protocol.http.server.file;

import java.net.URL;

/**
 * Resolve the URL for a request URI from the classpath
 * 
 * @author elandau
 *
 */
public class ClassPathURLResolver implements URLResolver {
    private static final String DEFAULT_PATH_PREFIX = "WEB-INF";
    
    private final String prefix;
    
    public ClassPathURLResolver() {
        this(DEFAULT_PATH_PREFIX);
    }
    
    public ClassPathURLResolver(String prefix) {
        this.prefix = prefix;
    }
     
    @Override
    public URL getUrl(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(prefix + path);
    }
}
