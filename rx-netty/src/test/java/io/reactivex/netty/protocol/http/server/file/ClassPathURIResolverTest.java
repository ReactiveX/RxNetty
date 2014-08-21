package io.reactivex.netty.protocol.http.server.file;

import junit.framework.Assert;

import org.junit.Test;

public class ClassPathURIResolverTest {
    @Test
    public void shouldReturnNullOnNotFound() {
        ClassPathURIResolver resolver = new ClassPathURIResolver();
        Assert.assertNull(resolver.getUri("sample_not_found.json"));
    }
    
    @Test
    public void shouldFindResourceURI() {
        ClassPathURIResolver resolver = new ClassPathURIResolver();
        Assert.assertNotNull(resolver.getUri("/sample.json"));
    }
}
