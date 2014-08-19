package io.reactivex.netty.protocol.http.server.file;

import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class LocalDirectoryURLResolver implements URLResolver {
    private final String prefix;
    
    public LocalDirectoryURLResolver() {
        this(SystemPropertyUtil.get("user.dir") + File.separator);
    }
    
    public LocalDirectoryURLResolver(String prefix) {
        this.prefix = prefix;
    }
     
    @Override
    public URL getUrl(String path) {
        try {
            return new URL("file:///" + prefix + path);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
