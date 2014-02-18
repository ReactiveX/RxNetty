package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.testng.Assert;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class TestUri {

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
        Assert.assertEquals(request.getUri(), uri, "Unexpected uri string");
        Assert.assertEquals(request.getQueryString(), queryString,  "Unexpected query string");
        Assert.assertEquals(request.getPath(), path, "Unexpected path string");
        Map<String,List<String>> qpsGot = request.getQueryParameters();
        Assert.assertNotNull(qpsGot, "Got null query parameters");
        Assert.assertEquals(qpsGot.size(), 2, "Unexpected number of query parameters");
        List<String> qp1Got = qpsGot.get(qp1Name);
        Assert.assertNotNull(qp1Got, "Got no query parameters with name: " + qp1Name);
        Assert.assertEquals(qp1Got.size(), 1, "Unexpected number of query parameters with name: " + qp1Name);
        Assert.assertEquals(qp1Got.get(0), qp1Val, "Unexpected query parameter value with name: " + qp1Name);

        List<String> qp2Got = qpsGot.get(qp2Name);
        Assert.assertNotNull(qp2Got, "Got no query parameters with name: " + qp2Name);
        Assert.assertEquals(qp2Got.size(), 2, "Unexpected number of query parameters with name: " + qp2Name);
        Assert.assertEquals(qp2Got.get(0), qp2Val, "Unexpected query parameter value with name: " + qp2Name);
        Assert.assertEquals(qp2Got.get(1), qp2Val2, "Unexpected query parameter second value with name: " + qp2Name);
    }

    @Test
    public void testEmptyQueryString() throws Exception {
        String path = "a/b/c";
        String uri = path + '?';
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpRequest<ByteBuf> request = new HttpRequest<ByteBuf>(nettyRequest, PublishSubject.<ByteBuf>create());
        Assert.assertEquals(request.getUri(), uri, "Unexpected uri string");
        Assert.assertEquals(request.getQueryString(), "",  "Unexpected query string");
    }

    @Test
    public void testAbsentQueryString() throws Exception {
        String path = "a/b/c";
        String uri = path;
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        HttpRequest<ByteBuf> request = new HttpRequest<ByteBuf>(nettyRequest, PublishSubject.<ByteBuf>create());
        Assert.assertEquals(request.getUri(), uri, "Unexpected uri string");
        Assert.assertEquals(request.getQueryString(), "",  "Unexpected query string");
    }
}
