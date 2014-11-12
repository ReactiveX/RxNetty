/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http.server.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            logger.debug("Error resolving uri for '{}'", resourcePath);
            return null;
        }
    }
}
