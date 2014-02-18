package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;
import org.testng.Assert;
import rx.subjects.PublishSubject;

import java.util.Map;
import java.util.Set;

/**
 * @author Nitesh Kant
 */
public class CookieTest {

    @Test
    public void testGetCookie() throws Exception {
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        String cookie1Domain = ".google.com";
        String cookie1Path = "/";
        String cookie1Header = cookie1Name + '=' + cookie1Value
                               + "; expires=Thu, 18-Feb-2016 07:47:08 GMT; path=" + cookie1Path + "; domain=" + cookie1Domain;
        nettyRequest.headers().add(HttpHeaders.Names.COOKIE, cookie1Header);
        HttpRequest<ByteBuf> request = new HttpRequest<ByteBuf>(nettyRequest, PublishSubject.<ByteBuf>create());
        Map<String,Set<Cookie>> cookies = request.getCookies();
        Assert.assertEquals(cookies.size(), 1, "Unexpected number of cookies.");
        Set<Cookie> cookies1 = cookies.get(cookie1Name);
        Assert.assertNotNull(cookies1, "No cookie found with name: " + cookie1Name);
        Assert.assertEquals(cookies1.size(), 1, "Unexpected number of cookies with name: " + cookie1Name);
        Cookie cookie = cookies1.iterator().next();
        Assert.assertEquals(cookie.getName(), cookie1Name, "Unexpected cookie name.");
        Assert.assertEquals(cookie.getPath(), cookie1Path, "Unexpected cookie path.");
        Assert.assertEquals(cookie.getDomain(), cookie1Domain, "Unexpected cookie domain.");
    }

    @Test
    public void testSetCookie() throws Exception {
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        HttpResponse<ByteBuf> response = new HttpResponse<ByteBuf>(new NoOpChannelHandlerContext(), HttpVersion.HTTP_1_1,
                                                                   nettyResponse);
        String cookieName = "name";
        String cookieValue = "value";
        response.addCookie(new DefaultCookie(cookieName, cookieValue));
        String cookieHeader = nettyResponse.headers().get(HttpHeaders.Names.SET_COOKIE);
        Assert.assertNotNull(cookieHeader, "Cookie header not found.");
        Set<Cookie> decode = CookieDecoder.decode(cookieHeader);
        Assert.assertNotNull(decode, "Decoded cookie not found.");
        Assert.assertEquals(decode.size(), 1, "Unexpected number of decoded cookie not found.");
        Cookie cookie = decode.iterator().next();
        Assert.assertEquals(cookie.getName(), cookieName, "Unexpected cookie name.");
        Assert.assertEquals(cookie.getValue(), cookieValue, "Unexpected cookie value.");

    }
}
