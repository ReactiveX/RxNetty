package io.reactivex.netty.protocol.http.server.file;

public class WebappFileRequestHandler extends ClassPathFileRequestHandler {
    public WebappFileRequestHandler() {
        super("WEB-INF");
    }
}
