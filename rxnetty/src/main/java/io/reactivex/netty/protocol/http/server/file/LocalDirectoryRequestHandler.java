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

import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

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
            logger.debug("Error resolving uri for '{}'", filename);
            return null;
        }
    }
}
