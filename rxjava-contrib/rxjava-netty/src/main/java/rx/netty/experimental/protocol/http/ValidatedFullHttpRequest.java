package rx.netty.experimental.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

/**
 * A wrapper of {@link DefaultFullHttpRequest} that validates given URL, as well as sets
 * up necessary header information
 */
public class ValidatedFullHttpRequest extends DefaultFullHttpRequest {
    private final UriInfo uriInfo;

    public static ValidatedFullHttpRequest get(URI uri) {
        return get(uri.toString());
    }

    public static ValidatedFullHttpRequest get(String uri) {
        return new ValidatedFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    }

    public static ValidatedFullHttpRequest post(String uri, ByteBuf content) {
        return new ValidatedFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri, content);
    }

    public static ValidatedFullHttpRequest post(URI uri, ByteBuf content) {
        return post(uri.toString(), content);
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
        this.uriInfo = UriInfo.fromUri(uri);

        init();
    }

    public ValidatedFullHttpRequest(HttpVersion version, HttpMethod method, URI uri) {
        this(version, method, uri.toString());
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content) {
        super(httpVersion, method, uri, content);
        this.uriInfo = UriInfo.fromUri(uri);

        init();
    }

    public ValidatedFullHttpRequest(HttpVersion httpVersion, HttpMethod method, URI uri, ByteBuf content) {
        this(httpVersion, method, uri.toString(), content);
    }

    private void init() {

        setUri(uriInfo.rawRelative());
        headers().set(HttpHeaders.Names.HOST, uriInfo.getHost());
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }
}