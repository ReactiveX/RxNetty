package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.junit.Test;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class UriTest {

    @Test
    public void testRequestUri() throws Exception {
        String path = "a/b/c";
        String qp1Name = "qp1";
        String qp1Val = "qp1Val";
        String qp2Name = "qp2";
        String qp2Val = "qp2Val";
        String qp2Val2 = "qp2Val222";
        String queryString = qp1Name + '=' + qp1Val + '&' + qp2Name + '=' + qp2Val + '&' + qp2Name + '=' + qp2Val2 ;
        String uri = path + '?' + queryString;
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpRequest<ByteBuf> request = new HttpRequest<ByteBuf>(nettyRequest, PublishSubject.<ByteBuf>create());
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", queryString,request.getQueryString());
        Assert.assertEquals("Unexpected path string", path, request.getPath());
        Map<String,List<String>> qpsGot = request.getQueryParameters();
        Assert.assertNotNull("Got null query parameters", qpsGot);
        Assert.assertEquals("Unexpected number of query parameters", 2, qpsGot.size());
        List<String> qp1Got = qpsGot.get(qp1Name);
        Assert.assertNotNull("Got no query parameters with name: " + qp1Name, qp1Got);
        Assert.assertEquals("Unexpected number of query parameters with name: " + qp1Name, 1, qp1Got.size());
        Assert.assertEquals("Unexpected query parameter value with name: " + qp1Name, qp1Val, qp1Got.get(0));

        List<String> qp2Got = qpsGot.get(qp2Name);
        Assert.assertNotNull("Got no query parameters with name: " + qp2Name, qp2Got);
        Assert.assertEquals("Unexpected number of query parameters with name: " + qp2Name, 2, qp2Got.size());
        Assert.assertEquals("Unexpected query parameter value with name: " + qp2Name, qp2Got.get(0), qp2Val);
        Assert.assertEquals("Unexpected query parameter second value with name: " + qp2Name, qp2Got.get(1), qp2Val2);
    }

    @Test
    public void testEmptyQueryString() throws Exception {
        String path = "a/b/c";
        String uri = path + '?';
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpRequest<ByteBuf> request = new HttpRequest<ByteBuf>(nettyRequest, PublishSubject.<ByteBuf>create());
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", "", request.getQueryString());
    }

    @Test
    public void testAbsentQueryString() throws Exception {
        String path = "a/b/c";
        String uri = path;
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpRequest<ByteBuf> request = new HttpRequest<ByteBuf>(nettyRequest, PublishSubject.<ByteBuf>create());
        Assert.assertEquals("Unexpected uri string", uri, request.getUri());
        Assert.assertEquals("Unexpected query string", "", request.getQueryString());
    }
}
