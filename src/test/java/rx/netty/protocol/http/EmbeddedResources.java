/**
 *
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package rx.netty.protocol.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.junit.Ignore;

@Ignore
@Path("/test")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EmbeddedResources {

    public static final List<String> smallStreamContent = new ArrayList<String>();

    public static final List<String> largeStreamContent = new ArrayList<String>();

    static {
        for (int i = 0; i < 3; i++) {
            smallStreamContent.add("line " + i);
        }
        for (int i = 0; i < 1000; i++) {
            largeStreamContent.add("line " + i);
        }
    }
    
    @GET
    @Path("/singleEntity")
    public Response getSingleEntity() throws IOException {
        return Response.ok("Hello world").build();
    }
    
    @GET
    @Path("/stream")
    @Produces("text/event-stream")
    public StreamingOutput getStream() {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                for (String line: smallStreamContent) {
                    String eventLine = "data:" + line + "\n\n";
                    output.write(eventLine.getBytes("UTF-8"));
                }
            }
        };
    }

    @GET
    @Path("/largeStream")
    @Produces("text/event-stream")
    public StreamingOutput getLargeStream() {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                for (String line: largeStreamContent) {
                    String eventLine = "data:" + line + "\n\n";
                    output.write(eventLine.getBytes("UTF-8"));
                }
            }
        };
    }
    
    @GET
    @Path("/timeout")
    public Response simulateTimeout(@QueryParam("timeout") int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (Exception e) {
        }
        return Response.ok().build();
    }
}
