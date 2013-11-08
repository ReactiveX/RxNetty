package rx.netty.experimental.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * A wrapper of {@link DefaultFullHttpRequest} that validates given URL, as well as sets
 * up necessary header information
 */
public class ValidatedFullHttpRequest extends DefaultFullHttpRequest {
    private final UriInfo uriInfo;

    public static ValidatedFullHttpRequest get(String uri) {
        return new ValidatedFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    }

    public static ValidatedFullHttpRequest post(String uri, ByteBuf content) {
        return new ValidatedFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri, content);
    }


    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
        this.uriInfo = UriInfo.fromUri(uri);

        init();
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content) {
        super(httpVersion, method, uri, content);
        this.uriInfo = UriInfo.fromUri(uri);

        init();
    }

    private void init() {

        setUri(uriInfo.rawRelative());
        headers().set(HttpHeaders.Names.HOST, uriInfo.getHost());
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }
}