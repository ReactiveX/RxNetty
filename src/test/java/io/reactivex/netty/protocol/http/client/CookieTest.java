package io.reactivex.netty.protocol.http.client;

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
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        String cookie1Domain = ".google.com";
        String cookie1Path = "/";
        String cookie1Header = cookie1Name + '=' + cookie1Value
                               + "; expires=Thu, 18-Feb-2016 07:47:08 GMT; path=" + cookie1Path + "; domain=" + cookie1Domain;
        nettyResponse.headers().add(HttpHeaders.Names.SET_COOKIE, cookie1Header);
        HttpResponse<ByteBuf> response = new HttpResponse<ByteBuf>(nettyResponse, PublishSubject.<ByteBuf>create());
        Map<String,Set<Cookie>> cookies = response.getCookies();
        Assert.assertNotNull(cookies, "Cookies are null.");
        Assert.assertEquals(cookies.size(), 1, "Cookies are empty.");
        Set<Cookie> cookies1 = cookies.get(cookie1Name);
        Assert.assertNotNull(cookies1, "No cookies found with name: " + cookie1Name);
        Assert.assertEquals(cookies1.size(), 1, "Unexpected number of cookies found.");
        Cookie cookieFound = cookies1.iterator().next();
        Assert.assertEquals(cookieFound.getName(), cookie1Name, "unexpected cookie name.");
        Assert.assertEquals(cookieFound.getValue(), cookie1Value, "unexpected cookie value.");
        Assert.assertEquals(cookieFound.getPath(), cookie1Path, "unexpected cookie path.");
        Assert.assertEquals(cookieFound.getDomain(), cookie1Domain, "unexpected cookie domain.");
    }

    @Test
    public void testSetCookie() throws Exception {
        DefaultHttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
        String cookie1Name = "PREF";
        String cookie1Value = "ID=a95756377b78e75e:FF=0:TM=1392709628:LM=1392709628:S=a5mOVvTB7DBkexgi";
        String cookie1Domain = ".google.com";
        String cookie1Path = "/";
        Cookie cookie = new DefaultCookie(cookie1Name, cookie1Value);
        cookie.setPath(cookie1Path);
        cookie.setDomain(cookie1Domain);
        new HttpRequest<ByteBuf>(nettyRequest).withCookie(cookie);
        String cookieHeader = nettyRequest.headers().get(HttpHeaders.Names.COOKIE);
        Assert.assertNotNull(cookieHeader, "No cookie header found.");
        Set<Cookie> decodeCookies = CookieDecoder.decode(cookieHeader);
        Assert.assertNotNull(decodeCookies, "No cookie found with name.");
        Assert.assertEquals(decodeCookies.size(), 1, "Unexpected number of cookies.");
        Cookie decodedCookie = decodeCookies.iterator().next();
        Assert.assertEquals(decodedCookie.getName(), cookie1Name, "Unexpected cookie name.");
        Assert.assertEquals(decodedCookie.getPath(), cookie1Path, "Unexpected cookie path.");
        Assert.assertEquals(decodedCookie.getDomain(), cookie1Domain, "Unexpected cookie domain.");
    }
}
